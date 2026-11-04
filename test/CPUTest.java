import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * CPU tests: Klaus Dormann's 6502/65C02 functional test suites, Bruce Clark's
 * decimal mode test (65C02 config, checks A, N, V, Z and C), and cycle counts.
 *
 * From the repository root:
 *   javac -d test/build *.java test/CPUTest.java
 *   java -cp test/build CPUTest
 *
 * Klaus Dormann's binaries (GPL-3.0) are downloaded into test/bin on first run.
 */
public class CPUTest {

    private static final String KLAUS_URL = "https://raw.githubusercontent.com/Klaus2m5/6502_65C02_functional_tests/master/bin_files/";
    private static final Path TEST_DIR = Paths.get("test");
    private static final Path BIN_DIR = TEST_DIR.resolve("bin");
    private static final long MAX_STEPS = 500_000_000L;

    private static int failures = 0;

    private static class Machine {
        final Bus bus = new Bus();
        final RAM ram = new RAM(bus, 0x0000, 0xFFFF);
        final CPU cpu = new CPU(bus);

        Machine() {
            AddressDecoder decoder = new AddressDecoder();
            decoder.addDevice(ram, 0x0000, 0xFFFF);
            bus.setAddressDecoder(decoder);
        }

        void load(byte[] bytes, int addr) {
            for (int i = 0; i < bytes.length; i++) {
                bus.write(addr + i, bytes[i] & 0xFF);
            }
        }

        void load(int addr, int... bytes) {
            for (int i = 0; i < bytes.length; i++) {
                bus.write(addr + i, bytes[i]);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        runTrapTest("6502 functional test", klausBin("6502_functional_test.bin"), 0x0400, 0x3469);
        runTrapTest("65C02 extended opcodes test", klausBin("65C02_extended_opcodes_test.bin"), 0x0400, 0x24F1);
        runDecimalTest();
        runCycleTests();
        runInterruptTests();

        if (failures > 0) {
            System.out.println(failures + " test(s) FAILED");
            System.exit(1);
        }
        System.out.println("All CPU tests passed");
    }

    private static Path klausBin(String name) throws IOException {
        Path path = BIN_DIR.resolve(name);
        if (!Files.isRegularFile(path)) {
            Files.createDirectories(BIN_DIR);
            System.out.println("Downloading " + name + "...");
            try (InputStream in = URI.create(KLAUS_URL + name).toURL().openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return path;
    }

    /** Runs a test that signals success or failure by jumping/branching to itself. */
    private static void runTrapTest(String name, Path bin, int start, int success) throws IOException {
        Machine m = new Machine();
        m.load(Files.readAllBytes(bin), 0x0000);
        m.cpu.setPC(start);

        for (long steps = 0; steps < MAX_STEPS; steps++) {
            int pc = m.cpu.getPC();
            m.cpu.step();
            if (m.cpu.isPaused()) {
                m.cpu.resume();           // $BB is the emulator breakpoint but a 1-byte NOP on the WDC 65C02
            }
            if (m.cpu.getPC() == pc) {
                report(name, pc == success, String.format("trapped at $%04X (success is $%04X)", pc, success));
                return;
            }
        }
        report(name, false, "no trap after " + MAX_STEPS + " steps");
    }

    private static void runDecimalTest() throws IOException {
        String name = "65C02 decimal mode test";
        Machine m = new Machine();
        m.load(Files.readAllBytes(TEST_DIR.resolve("65C02_decimal_test.bin")), 0x0200);
        m.cpu.setPC(0x0200);

        for (long steps = 0; steps < MAX_STEPS; steps++) {
            m.cpu.step();
            if (m.cpu.isStopped()) {
                int error = m.bus.debugRead(0x0B);
                int n1 = m.bus.debugRead(0x00);
                int n2 = m.bus.debugRead(0x01);
                report(name, error == 0, String.format("ERROR=%d, last operands N1=$%02X N2=$%02X", error, n1, n2));
                return;
            }
        }
        report(name, false, "STP not reached after " + MAX_STEPS + " steps");
    }

    /** Each case runs the setup instructions, then checks the cycles taken by the next instruction. */
    private static void runCycleTests() {
        // LDX #$01 / LDY #$01 set up indexing; $10FF + 1 crosses a page
        cycleCase("lda abs,x (no page cross)", 4, new int[] {0xA2, 0x01}, 0xBD, 0x00, 0x10);
        cycleCase("lda abs,x (page cross)", 5, new int[] {0xA2, 0x01}, 0xBD, 0xFF, 0x10);
        cycleCase("lda abs,y (page cross)", 5, new int[] {0xA0, 0x01}, 0xB9, 0xFF, 0x10);
        cycleCase("lda (zp),y (page cross)", 6, new int[] {0xA0, 0x01, 0xA9, 0xFF, 0x85, 0x20, 0xA9, 0x10, 0x85, 0x21}, 0xB1, 0x20);
        cycleCase("lda (zp)", 5, new int[] {}, 0xB2, 0x20);
        cycleCase("sta abs,x (page cross)", 5, new int[] {0xA2, 0x01}, 0x9D, 0xFF, 0x10);
        cycleCase("asl abs,x (no page cross)", 6, new int[] {0xA2, 0x01}, 0x1E, 0x00, 0x10);
        cycleCase("asl abs,x (page cross)", 7, new int[] {0xA2, 0x01}, 0x1E, 0xFF, 0x10);
        cycleCase("inc abs,x", 7, new int[] {0xA2, 0x01}, 0xFE, 0x00, 0x10);
        cycleCase("stz zp", 3, new int[] {}, 0x64, 0x20);
        cycleCase("stz abs,x", 5, new int[] {}, 0x9E, 0x00, 0x10);
        cycleCase("jmp (abs)", 6, new int[] {}, 0x6C, 0x00, 0x10);
        cycleCase("tsb zp", 5, new int[] {}, 0x04, 0x20);
        cycleCase("bit abs,x (page cross)", 5, new int[] {0xA2, 0x01}, 0x3C, 0xFF, 0x10);
        cycleCase("bne not taken", 2, new int[] {0xA9, 0x00}, 0xD0, 0x10);
        cycleCase("bne taken", 3, new int[] {0xA9, 0x01}, 0xD0, 0x10);
        cycleCase("bra", 3, new int[] {}, 0x80, 0x10);
        cycleCase("adc # (binary)", 2, new int[] {}, 0x69, 0x01);
        cycleCase("adc # (decimal)", 3, new int[] {0xF8}, 0x69, 0x01);
        cycleCase("nop $5c", 8, new int[] {}, 0x5C, 0x00, 0x00);

        // The instruction after the branch is at $04FF, so a forward branch crosses into $05xx
        Machine m = new Machine();
        m.load(0x04FD, 0xD0, 0x10);
        m.cpu.setPC(0x04FD);
        m.cpu.step();
        report("cycles: bne taken (page cross)", m.cpu.getCycleCount() == 4, "took " + m.cpu.getCycleCount() + ", expected 4");
    }

    private static void runInterruptTests() {
        // Hardware IRQ: 7 cycles, clears D, sets I
        Machine m = new Machine();
        m.load(0xFFFE, 0x00, 0x30);
        m.load(0x0400, 0xF8, 0x58, 0xEA);   // SED, CLI, NOP
        m.cpu.setPC(0x0400);
        m.cpu.step();
        m.cpu.step();
        m.bus.addInterrupter(() -> true);
        long before = m.cpu.getCycleCount();
        m.cpu.step();
        boolean[] flags = m.cpu.getFlags();  // N, V, D, I, Z, C
        report("irq", m.cpu.getPC() == 0x3000 && m.cpu.getCycleCount() - before == 7 && !flags[2] && flags[3],
               String.format("PC=$%04X cycles=%d D=%b I=%b", m.cpu.getPC(), m.cpu.getCycleCount() - before, flags[2], flags[3]));

        // Reset: I set, D clear
        m = new Machine();
        m.load(0xFFFC, 0x00, 0x04);
        m.load(0x0400, 0xF8);               // SED
        m.cpu.setPC(0x0400);
        m.cpu.step();
        m.cpu.reset();
        flags = m.cpu.getFlags();
        report("reset", m.cpu.getPC() == 0x0400 && flags[3] && !flags[2],
               String.format("PC=$%04X I=%b D=%b", m.cpu.getPC(), flags[3], flags[2]));

        // WAI: waits for an IRQ; with I set, resumes at the next instruction without servicing it
        m = new Machine();
        m.load(0x0400, 0x78, 0xCB, 0xEA);   // SEI, WAI, NOP
        m.cpu.setPC(0x0400);
        m.cpu.step();
        m.cpu.step();
        long waitStart = m.cpu.getCycleCount();
        for (int i = 0; i < 10; i++) m.cpu.step();
        boolean waited = m.cpu.getPC() == 0x0402 && m.cpu.getCycleCount() > waitStart;
        m.bus.addInterrupter(() -> true);
        m.cpu.step();
        report("wai", waited && m.cpu.getPC() == 0x0403, String.format("waited=%b PC=$%04X", waited, m.cpu.getPC()));

        // STP: stops until reset
        m = new Machine();
        m.load(0xFFFC, 0x00, 0x04);
        m.load(0x0400, 0xDB, 0xEA);         // STP, NOP
        m.cpu.reset();
        m.cpu.step();
        m.cpu.step();
        boolean stopped = m.cpu.isStopped() && m.cpu.getPC() == 0x0401;
        m.cpu.reset();
        report("stp", stopped && !m.cpu.isStopped() && m.cpu.getPC() == 0x0400, "stopped=" + stopped);
    }

    private static void cycleCase(String name, int expected, int[] setup, int... instruction) {
        Machine m = new Machine();
        int addr = 0x0400 + setup.length;
        m.load(0x0400, setup);
        m.load(addr, instruction);
        m.cpu.setPC(0x0400);
        while (m.cpu.getPC() != addr) {
            m.cpu.step();
        }

        long before = m.cpu.getCycleCount();
        m.cpu.step();
        long cycles = m.cpu.getCycleCount() - before;
        report("cycles: " + name, cycles == expected, "took " + cycles + ", expected " + expected);
    }

    private static void report(String name, boolean passed, String detail) {
        System.out.println((passed ? "PASS  " : "FAIL  ") + name + (passed ? "" : ": " + detail));
        if (!passed) failures++;
    }
}
