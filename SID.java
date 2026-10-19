import java.io.IOException;
import java.io.RandomAccessFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Emulates the KiT's SID sound chip (6581 or 8580), clocked by the CPU at 1.8432 MHz.
 *
 * Each CPU cycle steps three oscillators (with sync, ring modulation, and noise), three
 * envelope generators (with the real chip's rate counters, so envelopes and the ADSR
 * delay bug behave like hardware), and a state-variable model of the analog filter.
 * The output is averaged down to 44.1 kHz, played through Java Sound, and can also be
 * recorded to a WAV file. With no sound output, the chip only runs when a program reads
 * the oscillator 3 or envelope 3 register (e.g. for random numbers).
 */
public class SID implements BusListener {

    public static final int CLOCK = 1843200;
    private static final int SAMPLE_RATE = 44100;
    private static final double CYCLES_PER_SAMPLE = (double) CLOCK / SAMPLE_RATE;

    private static final int RING_SIZE = 1 << 15;
    private static final int PREFILL = 1024;             // samples to buffer before (re)starting playback
    private static final int MAX_QUEUE = 4096;           // beyond this, drop samples (e.g. in turbo mode)
    private static final int CHUNK = 256;                // samples per write to the audio device

    // Envelope rate counter periods for each attack/decay/release setting
    private static final int[] RATE_PERIOD = {
        9, 32, 63, 95, 149, 220, 267, 313, 392, 977, 1954, 3126, 3907, 11720, 19532, 31251
    };
    private static final int ATTACK = 0, DECAY_SUSTAIN = 1, RELEASE = 2;

    // Control register bits
    private static final int GATE = 0x01, SYNC = 0x02, RING = 0x04, TEST = 0x08;
    private static final int TRI = 0x10, SAW = 0x20, PULSE = 0x40, NOISE = 0x80;

    private final Bus bus;
    private boolean is8580;
    private int busValue;

    // Voices (index 0-2). Voice v is synced and ring-modulated by voice SRC[v].
    private static final int[] SRC = {2, 0, 1};
    private final int[] freq = new int[3];
    private final int[] pw = new int[3];
    private final int[] ctrl = new int[3];
    private final int[] acc = new int[3];
    private final int[] noise = new int[3];
    private final boolean[] msbRising = new boolean[3];

    private final int[] attack = new int[3], decay = new int[3], sustain = new int[3], release = new int[3];
    private final int[] envState = new int[3];
    private final int[] envLevel = new int[3];
    private final int[] rateCounter = new int[3];
    private final int[] ratePeriod = new int[3];
    private final int[] expCounter = new int[3];
    private final int[] expPeriod = new int[3];
    private final boolean[] holdZero = new boolean[3];

    // Filter and volume
    private int fc, res, filt, modeVol;
    private double w0, invQ;
    private double vlp, vbp;

    // Analog details of each model
    private int waveZero;
    private int voiceDC;
    private double mixerDC;

    // Output
    private boolean audioActive;
    private long pendingCycles;
    private double sampleSum;
    private int sampleCount;
    private double untilSample = CYCLES_PER_SAMPLE;
    private double dcIn, dcOut;

    private final short[] ring = new short[RING_SIZE];
    private volatile int writePos, readPos;
    private SourceDataLine line;
    private RandomAccessFile wav;
    private final byte[] wavBuf = new byte[1 << 16];
    private int wavBufLen;
    private long wavSamples;

    public SID(Bus bus, int startAddr, int endAddr) {
        this.bus = bus;
        setModel8580(false);
        reset();
    }

    /** Emulate an 8580 instead of the default 6581. */
    public void setModel8580(boolean val) {
        is8580 = val;
        waveZero = is8580 ? 0x800 : 0x380;
        voiceDC = is8580 ? 0 : 0x800 * 0xff;
        mixerDC = is8580 ? 0 : -0x380 * 0xff * 2.0;
        updateFilter();
    }

    public void reset() {
        for (int v = 0; v < 3; v++) {
            freq[v] = pw[v] = ctrl[v] = acc[v] = 0;
            noise[v] = 0x7FFFF8;
            attack[v] = decay[v] = sustain[v] = release[v] = 0;
            envState[v] = RELEASE;
            envLevel[v] = 0;
            rateCounter[v] = 0;
            ratePeriod[v] = RATE_PERIOD[0];
            expCounter[v] = 0;
            expPeriod[v] = 1;
            holdZero[v] = true;
        }
        fc = res = filt = modeVol = 0;
        vlp = vbp = 0;
        updateFilter();
    }

    /** Play sound through the default audio device. Returns false if none is available. */
    public boolean startAudio() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, CHUNK * 2 * 4);
            line.start();
        } catch (Exception e) {
            System.out.println("No audio output (" + e.getMessage() + "); SID will be silent");
            line = null;
            return false;
        }

        audioActive = true;
        Thread t = new Thread(this::audioLoop, "SID audio");
        t.setDaemon(true);
        t.start();
        return true;
    }

    /** Also write everything the SID plays to a 16-bit mono WAV file. */
    public void startRecording(String path) throws IOException {
        wav = new RandomAccessFile(path, "rw");
        wav.setLength(0);
        wav.write(new byte[44]);
        audioActive = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::finishRecording));
    }

    /** Samples generated but not yet sent to the audio device. */
    public int queuedSamples() {
        return (writePos - readPos) & (RING_SIZE - 1);
    }

    @Override
    public void activate() {
        int reg = bus.getAddr() & 0x1F;

        if (bus.readBitSet()) {
            bus.setData(read(reg));
        } else {
            write(reg, bus.getData());
        }
    }

    @Override
    public int debugRead(int addr) {
        return busValue;
    }

    private int read(int reg) {
        switch (reg) {
            case 0x19:
            case 0x1A:
                return 0xFF;                            // no paddles connected
            case 0x1B:
                catchUp();
                return waveform(2) >> 4;
            case 0x1C:
                catchUp();
                return envLevel[2];
            default:
                return busValue;                        // write-only registers read back the last bus value
        }
    }

    private void write(int reg, int val) {
        busValue = val;
        if (reg > 0x18) return;

        if (reg < 0x15) {
            int v = reg / 7;
            switch (reg % 7) {
                case 0: freq[v] = (freq[v] & 0xFF00) | val; break;
                case 1: freq[v] = (freq[v] & 0x00FF) | (val << 8); break;
                case 2: pw[v] = (pw[v] & 0xF00) | val; break;
                case 3: pw[v] = (pw[v] & 0x0FF) | ((val & 0x0F) << 8); break;
                case 4: writeControl(v, val); break;
                case 5:
                    attack[v] = val >> 4;
                    decay[v] = val & 0x0F;
                    if (envState[v] == ATTACK) ratePeriod[v] = RATE_PERIOD[attack[v]];
                    else if (envState[v] == DECAY_SUSTAIN) ratePeriod[v] = RATE_PERIOD[decay[v]];
                    break;
                case 6:
                    sustain[v] = val >> 4;
                    release[v] = val & 0x0F;
                    if (envState[v] == RELEASE) ratePeriod[v] = RATE_PERIOD[release[v]];
                    break;
            }
            return;
        }

        switch (reg) {
            case 0x15: fc = (fc & 0x7F8) | (val & 0x07); updateFilter(); break;
            case 0x16: fc = (fc & 0x007) | (val << 3); updateFilter(); break;
            case 0x17: res = val >> 4; filt = val & 0x0F; updateFilter(); break;
            case 0x18: modeVol = val; break;
        }
    }

    private void writeControl(int v, int val) {
        boolean gateOn = (val & GATE) != 0;
        boolean wasOn = (ctrl[v] & GATE) != 0;

        if (gateOn && !wasOn) {
            envState[v] = ATTACK;
            ratePeriod[v] = RATE_PERIOD[attack[v]];
            holdZero[v] = false;
        } else if (!gateOn && wasOn) {
            envState[v] = RELEASE;
            ratePeriod[v] = RATE_PERIOD[release[v]];
        }

        if ((val & TEST) != 0) {
            acc[v] = 0;
            noise[v] = 0x7FFFF8;
        }
        ctrl[v] = val;
    }

    // Typical 6581 cutoff (Hz) at register values 0, 128, ... 2048: barely moves at the
    // bottom, then rises steeply through the middle. Real chips vary quite a bit.
    private static final double[] CUTOFF_6581 = {
        220, 230, 250, 300, 450, 800, 1600, 3200, 5000, 7000, 9500, 12000, 14000, 15500, 17000, 17600, 18000
    };

    private void updateFilter() {
        double f0;
        if (is8580) {
            f0 = 30 + fc * (12500.0 / 2047);
        } else {
            double x = fc / 128.0;
            int i = Math.min((int) x, CUTOFF_6581.length - 2);
            f0 = CUTOFF_6581[i] + (CUTOFF_6581[i + 1] - CUTOFF_6581[i]) * (x - i);
        }
        w0 = 2 * Math.PI * f0 / CLOCK;
        invQ = 1.0 / (0.707 + res / 15.0 * (is8580 ? 1.3 : 1.0));
    }

    /** Advance the chip by the given number of CPU cycles. */
    public void clock(int cycles) {
        if (!audioActive) {
            pendingCycles += cycles;
            return;
        }
        for (int i = 0; i < cycles; i++) {
            clockOne(true);
        }
    }

    private void catchUp() {
        // Without sound output, only run the chip when a program looks at it
        while (pendingCycles > 0) {
            clockOne(false);
            pendingCycles--;
        }
    }

    private void clockOne(boolean output) {
        // Oscillators
        for (int v = 0; v < 3; v++) {
            if ((ctrl[v] & TEST) != 0) {
                msbRising[v] = false;
                continue;
            }
            int prev = acc[v];
            acc[v] = (prev + freq[v]) & 0xFFFFFF;
            msbRising[v] = (~prev & acc[v] & 0x800000) != 0;
            if ((~prev & acc[v] & 0x080000) != 0) {
                int n = noise[v];
                int bit = ((n >> 22) ^ (n >> 17)) & 1;
                noise[v] = ((n << 1) & 0x7FFFFF) | bit;
            }
        }

        // Hard sync: a voice resets when its source's oscillator wraps, unless that
        // source was itself just reset by its own source
        for (int v = 0; v < 3; v++) {
            int s = SRC[v];
            if ((ctrl[v] & SYNC) != 0 && msbRising[s] && !((ctrl[s] & SYNC) != 0 && msbRising[SRC[s]])) {
                acc[v] = 0;
            }
        }

        for (int v = 0; v < 3; v++) {
            clockEnvelope(v);
        }

        if (!output) return;

        // Voices and filter
        double filtered = 0, direct = 0;
        for (int v = 0; v < 3; v++) {
            double out = (waveform(v) - waveZero) * envLevel[v] + voiceDC;
            if ((filt & (1 << v)) != 0) {
                filtered += out;
            } else if (v != 2 || (modeVol & 0x80) == 0) {
                direct += out;                          // bit 7 of mode/vol mutes an unfiltered voice 3
            }
        }

        double vhp = vbp * invQ - vlp - filtered;
        vbp -= w0 * vhp;
        vlp -= w0 * vbp;

        double mix = direct + mixerDC;
        if ((modeVol & 0x10) != 0) mix += vlp;
        if ((modeVol & 0x20) != 0) mix += vbp;
        if ((modeVol & 0x40) != 0) mix += vhp;
        mix *= modeVol & 0x0F;

        sampleSum += mix;
        sampleCount++;
        if (--untilSample <= 0) {
            untilSample += CYCLES_PER_SAMPLE;
            emitSample(sampleSum / sampleCount);
            sampleSum = 0;
            sampleCount = 0;
        }
    }

    private void clockEnvelope(int v) {
        // 15-bit rate counter: if the period is lowered below the count, it has to wrap
        // around first, which is the SID's "ADSR delay bug"
        if ((++rateCounter[v] & 0x8000) != 0) {
            rateCounter[v] = (rateCounter[v] + 1) & 0x7FFF;
        }
        if (rateCounter[v] != ratePeriod[v]) return;
        rateCounter[v] = 0;

        if (envState[v] != ATTACK && ++expCounter[v] != expPeriod[v]) return;
        expCounter[v] = 0;
        if (holdZero[v]) return;

        switch (envState[v]) {
            case ATTACK:
                envLevel[v] = (envLevel[v] + 1) & 0xFF;
                if (envLevel[v] == 0xFF) {
                    envState[v] = DECAY_SUSTAIN;
                    ratePeriod[v] = RATE_PERIOD[decay[v]];
                }
                break;
            case DECAY_SUSTAIN:
                if (envLevel[v] != sustain[v] * 0x11) envLevel[v]--;
                break;
            case RELEASE:
                envLevel[v] = (envLevel[v] - 1) & 0xFF;
                break;
        }

        // Decay and release slow down as the level falls, approximating an exponential curve
        switch (envLevel[v]) {
            case 0xFF: expPeriod[v] = 1; break;
            case 0x5D: expPeriod[v] = 2; break;
            case 0x36: expPeriod[v] = 4; break;
            case 0x1A: expPeriod[v] = 8; break;
            case 0x0E: expPeriod[v] = 16; break;
            case 0x06: expPeriod[v] = 30; break;
            case 0x00: expPeriod[v] = 1; holdZero[v] = true; break;
        }
    }

    /** 12-bit waveform output of voice v. Combined waveforms are approximated by ANDing. */
    private int waveform(int v) {
        int c = ctrl[v];
        int a = acc[v];
        int out = 0xFFF;
        boolean any = false;

        if ((c & TRI) != 0) {
            int msb = a & 0x800000;
            if ((c & RING) != 0) msb ^= acc[SRC[v]] & 0x800000;
            out &= ((msb != 0 ? ~a : a) >> 11) & 0xFFF;
            any = true;
        }
        if ((c & SAW) != 0) {
            out &= a >> 12;
            any = true;
        }
        if ((c & PULSE) != 0) {
            out &= ((c & TEST) != 0 || (a >> 12) >= pw[v]) ? 0xFFF : 0;
            any = true;
        }
        if ((c & NOISE) != 0) {
            int n = noise[v];
            out &= ((n >> 9) & 0x800) | ((n >> 8) & 0x400) | ((n >> 5) & 0x200) | ((n >> 3) & 0x100)
                 | ((n >> 2) & 0x080) | ((n << 1) & 0x040) | ((n << 3) & 0x020) | ((n << 4) & 0x010);
            any = true;
        }
        return any ? out : 0;
    }

    private void emitSample(double mix) {
        // Remove DC (the 6581's offsets), like the output capacitor on the board
        double y = mix - dcIn + 0.9972 * dcOut;
        dcIn = mix;
        dcOut = y;

        int s = (int) (y / 2200);
        if (s > 32767) s = 32767;
        if (s < -32768) s = -32768;

        if (line != null && queuedSamples() < MAX_QUEUE) {
            ring[writePos] = (short) s;
            writePos = (writePos + 1) & (RING_SIZE - 1);
        }

        if (wav != null) {
            record(s);
        }
    }

    private synchronized void record(int s) {
        if (wav == null) return;
        wavBuf[wavBufLen++] = (byte) s;
        wavBuf[wavBufLen++] = (byte) (s >> 8);
        wavSamples++;
        if (wavBufLen == wavBuf.length) {
            flushRecording();
        }
    }

    private void flushRecording() {
        try {
            wav.write(wavBuf, 0, wavBufLen);
        } catch (IOException e) {
            e.printStackTrace();
            wav = null;
        }
        wavBufLen = 0;
    }

    // The emulator and the sound card run off different clocks, so the buffer slowly drifts.
    // Wait for a little audio before playing, and again if it ever runs dry; samples
    // beyond MAX_QUEUE are dropped in emitSample.
    private void audioLoop() {
        byte[] buf = new byte[CHUNK * 2];
        short last = 0;
        boolean filling = true;
        while (true) {
            if (filling && queuedSamples() < PREFILL) {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    return;
                }
            } else {
                filling = false;
            }

            for (int i = 0; i < CHUNK; i++) {
                if (!filling && queuedSamples() > 0) {
                    last = ring[readPos];
                    readPos = (readPos + 1) & (RING_SIZE - 1);
                } else {
                    filling = true;
                }
                buf[2 * i] = (byte) last;
                buf[2 * i + 1] = (byte) (last >> 8);
            }
            line.write(buf, 0, buf.length);
        }
    }

    private synchronized void finishRecording() {
        if (wav == null) return;
        flushRecording();
        if (wav == null) return;
        try {
            long dataBytes = wavSamples * 2;
            wav.seek(0);
            wav.writeBytes("RIFF");
            writeLE(wav, 36 + dataBytes, 4);
            wav.writeBytes("WAVEfmt ");
            writeLE(wav, 16, 4);
            writeLE(wav, 1, 2);                         // PCM
            writeLE(wav, 1, 2);                         // mono
            writeLE(wav, SAMPLE_RATE, 4);
            writeLE(wav, SAMPLE_RATE * 2, 4);
            writeLE(wav, 2, 2);
            writeLE(wav, 16, 2);
            wav.writeBytes("data");
            writeLE(wav, dataBytes, 4);
            wav.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        wav = null;
    }

    private static void writeLE(RandomAccessFile f, long val, int bytes) throws IOException {
        for (int i = 0; i < bytes; i++) {
            f.write((int) (val >> (8 * i)) & 0xFF);
        }
    }
}
