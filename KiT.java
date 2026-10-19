import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class KiT extends JPanel {

    private final static File DEFAULT_LOAD_FILE = new File("/Users/tomlinsonk/projects/6502/kcc/out.prg");
    private final static String DEFAULT_HEADLESS_KEYS = "l\nr\n";

    private final static int RAM_START = 0x0000;
    private final static int RAM_END = 0x5FFF;

    private final static int VRAM_START = 0x6000;
    private final static int VRAM_END = 0x77FF;

    private final static int VIA1_START = 0x7800;
    private final static int VIA1_END = 0x78FF;

    private final static int VIA2_START = 0x7900;
    private final static int VIA2_END = 0x79FF;

    private final static int UART_START = 0x7A00;
    private final static int UART_END = 0x7AFF;

    private final static int SID_START = 0x7E00;
    private final static int SID_END = 0x7EFF;

    private final static int SSD_START = 0x8000;
    private final static int SSD_END = 0x8FFF;

    private final static int ROM_HIDDEN_START = 0x8000;

    private final static double NS_PER_CYCLE = 1e9 / 1_843_200;
    private final static int ROM_START = 0x9000;
    private final static int ROM_END = 0xFFFF;


    private Bus bus;
    private CPU cpu;
    private RAM ram;
    private ROM rom;
    private VIA via1;
    private VIA via2;
    private SSD ssd;

    // Dummy components
    private UART uart;

    private SID sid;

    private boolean doReset;
    private boolean debugging;

    private Graphics graphics;

    private JLabel clockLabel;
    private boolean turboMode;
    private boolean dumpScreenHex;

    // Cycles spent executing instructions in [countCyclesStart, countCyclesEnd] (off by default)
    private int countCyclesStart = -1;
    private int countCyclesEnd = -2;
    private long countedCycles = 0;

    private final Object pauseLock = new Object();



    private class Debugger {
        private JLabel registerLabel;
        private JLabel flagLabel;
        private JTextArea memoryView;

        private Debugger() {    
            createDebugWindow();
        }
    
        private void createDebugWindow() {
            JFrame frame = new JFrame("Debugger");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel panel = new JPanel();
    
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 1;
            gbc.weighty = 1;

            // Labels
            Font monoFont = new Font("Monospaced", Font.PLAIN, 12 );
            registerLabel = new JLabel();
            registerLabel.setFont(monoFont);
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(registerLabel, gbc);
            updateRegisterLabel();

            flagLabel = new JLabel();
            flagLabel.setFont(monoFont);
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.weightx = 10;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(flagLabel, gbc); 
            updateFlagLabel();

            // Reset weight
            gbc.weightx = 1;

            // Step button
            JButton stepButton = new JButton("Step");
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.gridheight = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            panel.add(stepButton, gbc); 

            // Memory view area
            JTextArea memoryView = new JTextArea(getMemText(0), 17, 54);
            memoryView.setFont(monoFont);
            JScrollPane scrollPane = new JScrollPane(memoryView);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            memoryView.setEditable(false);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridy = 3;
            gbc.gridwidth = 3;
            gbc.gridheight = 1;
            panel.add(scrollPane, gbc);

            // Memory start addr field
            JTextField memStartAddrField = new JTextField("0000", 4);
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(memStartAddrField, gbc);


            JButton memStartButton = new JButton("Go");
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(memStartButton, gbc); 
    
            memStartButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int startAddr = Integer.parseInt(memStartAddrField.getText(), 16);
                    memoryView.setText(getMemText(startAddr));
                }
            });
    
            stepButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cpu.step();
                    updateRegisterLabel();
                    updateFlagLabel();
                    int startAddr = Integer.parseInt(memStartAddrField.getText(), 16);
                    memoryView.setText(getMemText(startAddr));

                }
            });


    
            frame.setContentPane(panel);
    
            frame.setSize(410, 410);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    synchronized (pauseLock) {
                        cpu.resume();
                        debugging = false;
                        pauseLock.notifyAll();
                    }

                    frame.dispose();
                }
            });

        }

        protected String getMemText(int startAddr) {
            String s = "       0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f\n";
            for (int row = 0; row < 16; row++) {
                s += String.format("%04x:", startAddr + 16 * row);
                for (int col = 0; col < 16 && (startAddr + 16 * row + col) <= 0xffff; col++) {
                    s += String.format(" %02x", bus.debugRead(startAddr + 16 * row + col));
                    if (col == 7) s += " ";
                }
                s += "\n";
            }

            return s;
        }

        private void updateRegisterLabel() {
            registerLabel.setText(String.format("A: $%02X  X: $%02X  Y: $%02X  S: $%02X  PC: $%04X", 
                                                cpu.getA(), cpu.getX(), cpu.getY(), cpu.getS(), cpu.getPC()));
        }

        private void updateFlagLabel() {
            // N, V, D, I, Z, C
            boolean[] cpuFlags = cpu.getFlags();
            int[] flags = new int[cpuFlags.length];
            for (int i = 0; i < flags.length; i++) {
                flags[i] = cpuFlags[i] ? 1 : 0;
            }

            flagLabel.setText(String.format("N: %d  Z: %d  C: %d  V: %d  D: %d  I: %d  prev: %s",
                                            flags[0], flags[4], flags[5], flags[1], flags[2], flags[3],
                                            cpu.getLastInstructionMnemonic()));
        }
    }

    public KiT() {
        KeyListener listener = new PS2KeyListender();

		addKeyListener(listener);
		setFocusable(true);

        bus = new Bus();
        cpu = new CPU(bus);
        ram = new RAM(bus, RAM_START, RAM_END);
        rom = new ROM(bus, ROM_HIDDEN_START, ROM_END);
        via1 = new VIA(bus, VIA1_START, VIA1_END);
        via2 = new VIA(bus, VIA2_START, VIA2_END);

        uart = new UART(bus, UART_START, UART_END);
        sid = new SID(bus, SID_START, SID_END);
        ssd = new SSD(bus, via2, SSD_START, SSD_END);

        graphics = new Graphics(bus, via2, VRAM_START, VRAM_END);

        AddressDecoder addrDecoder = new AddressDecoder();

        addrDecoder.addDevice(ram, RAM_START, RAM_END);
        addrDecoder.addDevice(rom, ROM_START, ROM_END);
        addrDecoder.addDevice(via1, VIA1_START, VIA1_END);
        addrDecoder.addDevice(via2, VIA2_START, VIA2_END);
        addrDecoder.addDevice(graphics, VRAM_START, VRAM_END);
        addrDecoder.addDevice(uart, UART_START, UART_END);
        addrDecoder.addDevice(sid, SID_START, SID_END);
        addrDecoder.addDevice(ssd, SSD_START, SSD_END);

        bus.setAddressDecoder(addrDecoder);
        bus.addInterrupter(via1);
        bus.addInterrupter(via2);

        turboMode = false;
        debugging = false;
        dumpScreenHex = false;

        cpu.reset();
        // cpu.printStatus();
    }

    private class PS2KeyListender implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            pressKey(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            releaseKey(e.getKeyCode());

        }
    }

    private void pressKey(int keyCode) {
        if (PS2.isExtended(keyCode)) {
            via1.kbByte(PS2.EXTENDED_CODE);
            kbByteDelay();
        }

        via1.kbByte(PS2.getScanCode(keyCode));
    }

    private void releaseKey(int keyCode) {
        via1.kbByte(PS2.RELEASE_CODE);
        kbByteDelay();

        if (PS2.isExtended(keyCode)) {
            via1.kbByte(PS2.EXTENDED_CODE);
            kbByteDelay();
        }

        via1.kbByte(PS2.getScanCode(keyCode));
    }

    private void typeHeadlessKeys(String keys) {
        for (int i = 0; i < keys.length(); i++) {
            int keyCode = keyCodeFor(keys.charAt(i));
            if (keyCode == KeyEvent.VK_UNDEFINED) {
                System.out.println("Warning: skipping unsupported headless key " + keys.charAt(i));
                continue;
            }

            pressKey(keyCode);
            kbByteDelay();
            releaseKey(keyCode);
            kbByteDelay();
        }
    }

    private int keyCodeFor(char c) {
        if (c >= 'a' && c <= 'z') {
            return KeyEvent.VK_A + c - 'a';
        }

        if (c >= 'A' && c <= 'Z') {
            return KeyEvent.VK_A + c - 'A';
        }

        if (c >= '0' && c <= '9') {
            return KeyEvent.VK_0 + c - '0';
        }

        switch (c) {
            case ' ':
                return KeyEvent.VK_SPACE;
            case '\n':
            case '\r':
                return KeyEvent.VK_ENTER;
            case '\b':
                return KeyEvent.VK_BACK_SPACE;
            case '\t':
                return KeyEvent.VK_TAB;
            case ',':
                return KeyEvent.VK_COMMA;
            case ';':
                return KeyEvent.VK_SEMICOLON;
            case '.':
                return KeyEvent.VK_PERIOD;
            case '/':
                return KeyEvent.VK_SLASH;
            case '\'':
                return KeyEvent.VK_QUOTE;
            case '=':
                return KeyEvent.VK_EQUALS;
            case '-':
                return KeyEvent.VK_MINUS;
            case '[':
                return KeyEvent.VK_OPEN_BRACKET;
            case ']':
                return KeyEvent.VK_CLOSE_BRACKET;
            case '\\':
                return KeyEvent.VK_BACK_SLASH;
            case '`':
                return KeyEvent.VK_BACK_QUOTE;
            default:
                return KeyEvent.VK_UNDEFINED;
        }
    }


    private void run(boolean headlessMode) {
        new Thread(new Runnable() {
			@Override
			public void run() {
				long currTime;
                long prevCycleCount = 0;
                long cycleCount = 0;
                int newCycles;

                long checkpointTime = 0;
                long checkpointCycles = 0;

                double schedule = System.nanoTime();     // when the current cycle should happen
                while (true) {
                    synchronized (pauseLock) {
                        if (doReset) {
                            via1.reset();
                            via2.reset();
                            sid.reset();
                            cpu.reset();
                            doReset = false;
                            prevCycleCount = 0;
                            checkpointTime = 0;
                            checkpointCycles = 0;
                        } else if (cpu.isPaused()) {
                            if (headlessMode) {
                                dumpHeadlessState("Breakpoint reached");
                                ssd.writeFile();
                                System.exit(0);
                            }

                            if (!debugging) {
                                startDebugger();
                            }
                            try {
                                pauseLock.wait();
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }

                    currTime = System.nanoTime();

                    int stepPC = cpu.getPC();
                    cpu.step();

                    cycleCount = cpu.getCycleCount();
                    newCycles = (int)(cycleCount - prevCycleCount);

                    if (stepPC >= countCyclesStart && stepPC <= countCyclesEnd) {
                        countedCycles += newCycles;
                    }
                    
                    sid.clock(newCycles);

                    if (!turboMode) {
                        // Run at the KiT's 1.8432 MHz
                        schedule += newCycles * NS_PER_CYCLE;
                        if (currTime - schedule > 20_000_000) {
                            schedule = currTime;                // fell far behind (e.g. a GC pause): don't race to catch up
                        }
                        while (System.nanoTime() < schedule) {
                            continue;
                        }
                    } else {
                        schedule = System.nanoTime();
                    }

                    prevCycleCount = cycleCount;
                    // System.out.println(cycleCount);

                    via1.updateCycleCount(newCycles);
                    via2.updateCycleCount(newCycles);

                    if (clockLabel != null && currTime - checkpointTime > 2_000_000_000) {
                        clockLabel.setText(String.format("%.2f MHz", ((cycleCount - checkpointCycles) / 2_000_000.0)));
                        
                        checkpointTime = currTime;
                        checkpointCycles = cycleCount;
                    }
                }
			}


		}).start();
    }

    private void run() {
        run(false);
    }

    private void dumpHeadlessState(String reason) {
        System.out.println(reason);
        cpu.printStatus();
        if (countCyclesStart <= countCyclesEnd) {
            System.out.println("Cycles: " + countedCycles);
        }
        System.out.println("Screen:");
        System.out.print(getScreenText());
        if (dumpScreenHex) {
            System.out.println("Screen hex:");
            System.out.print(getScreenHex());
        }
    }

    private String getScreenText() {
        String text = "";
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 32; col++) {
                int val = bus.debugRead(VRAM_START + row * 32 + col) & 0x7F;
                if (val >= 1 && val <= 26) {
                    text += (char)('a' + val - 1);
                } else if (val >= 32 && val <= 126) {
                    text += (char) val;
                } else {
                    text += ' ';
                }
            }
            text += "\n";
        }

        return text;
    }

    private String getScreenHex() {
        String text = "";
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 32; col++) {
                text += String.format("%02X", bus.debugRead(VRAM_START + row * 32 + col));
                if (col < 31) text += " ";
            }
            text += "\n";
        }

        return text;
    }

    private void startHeadlessKeys(String keys) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }

                typeHeadlessKeys(keys);
            }
        }).start();
    }

    private void startHeadlessTimeout(long timeoutMs) {
        if (timeoutMs <= 0) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeoutMs);
                } catch (InterruptedException e) {
                    return;
                }

                dumpHeadlessState("Headless timeout after " + timeoutMs + " ms");
                ssd.writeFile();
                System.exit(2);
            }
        }).start();
    }

    private static void loadDefaultPrg(KiT kit) {
        if (DEFAULT_LOAD_FILE.isFile()) {
            kit.startLoad(DEFAULT_LOAD_FILE);
        } else {
            System.out.println("Warning: couldn't find load file " + DEFAULT_LOAD_FILE + ". Update DEFAULT_LOAD_FILE in KiT.java to load a .prg file");
        }
    }

    private static boolean isHeadlessMode(String[] args) {
        for (String arg : args) {
            if (arg.equals("--headless")) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasArg(String[] args, String name) {
        for (String arg : args) {
            if (arg.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private static String getArgValue(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }

        return defaultValue;
    }

    private static long getLongArgValue(String[] args, String name, long defaultValue) {
        String value = getArgValue(args, name, null);
        if (value == null) return defaultValue;

        return Long.parseLong(value);
    }

    private static String getHeadlessKeys(String[] args) {
        return getArgValue(args, "--keys", DEFAULT_HEADLESS_KEYS).replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    private void startDebugger() {
        debugging = true;
        Debugger debugger = new Debugger();
    }   

    private void kbByteDelay() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    } 

    public BufferedImage getDisplayImage() {
        graphics.updateImage();
        return graphics.getImage();
    }

    public void reset() {
        doReset = true;
    }

    public void setClockLabel(JLabel label) {
        clockLabel = label;
    }

    public void setTurboMode(boolean val) {
        turboMode = val;
    }

    public void setDumpScreenHex(boolean val) {
        dumpScreenHex = val;
    }

    public void setCountCyclesRange(int start, int end) {
        countCyclesStart = start;
        countCyclesEnd = end;
    }

    public void startLoad(File f) {
        uart.loadPrg(f);
    }

    public void setSaveSsd(boolean val) {
        ssd.setSaveEnabled(val);
    }

    /** Set up the SID from the command line: --sid-model 8580, --record-sid out.wav, --no-sound */
    private void configureSid(String[] args, boolean headless) {
        sid.setModel8580("8580".equals(getArgValue(args, "--sid-model", "6581")));

        String wavPath = getArgValue(args, "--record-sid", null);
        if (wavPath != null) {
            try {
                sid.startRecording(wavPath);
            } catch (java.io.IOException e) {
                System.out.println("Error: couldn't write " + wavPath + ": " + e.getMessage());
                System.exit(1);
            }
        }

        if (!headless && !hasArg(args, "--no-sound")) {
            sid.startAudio();
        }
    }

    public void onExit() {
        ssd.writeFile();
        System.exit(0);
    }

    public static void addSpeedButtons(JPanel controlPanel, KiT kit) {
        JRadioButton slowButton = new JRadioButton("Realtime");
        slowButton.setSelected(true);
        slowButton.setFocusable(false);

        JRadioButton fastButton = new JRadioButton("Turbo");
        fastButton.setFocusable(false);

        // Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(slowButton);
        group.add(fastButton);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                kit.setTurboMode(e.getActionCommand().equals("Turbo"));
            }
        };

        // Register a listener for the radio buttons.
        slowButton.addActionListener(listener);
        fastButton.addActionListener(listener);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor =  GridBagConstraints.WEST;
        gbc.gridy = 3;
        controlPanel.add(slowButton, gbc);
        gbc.gridy = 4;
        controlPanel.add(fastButton, gbc);
    }

    public static void main(String[] args) {

        // System.out.println("RAM:");
        // for (int j = 0xFD; j < 0x0120; j++) {
        //     System.out.println(j + ": " + ram.get(j));
        // }

        KiT kit = new KiT();
        kit.setSaveSsd(!hasArg(args, "--no-save-ssd"));
        kit.configureSid(args, isHeadlessMode(args));

        if (isHeadlessMode(args)) {
            kit.setTurboMode(true);
            kit.setDumpScreenHex(hasArg(args, "--dump-screen-hex"));
            String cycleRange = getArgValue(args, "--count-cycles", null);
            if (cycleRange != null) {
                String[] bounds = cycleRange.split("-");
                kit.setCountCyclesRange(Integer.parseInt(bounds[0], 16), Integer.parseInt(bounds[1], 16));
            }
            File prg = new File(getArgValue(args, "--prg", DEFAULT_LOAD_FILE.getPath()));
            if (!prg.isFile()) {
                System.out.println("Error: couldn't find load file " + prg);
                System.exit(1);
            }
            kit.startLoad(prg);
            kit.startHeadlessKeys(getHeadlessKeys(args));
            kit.startHeadlessTimeout(getLongArgValue(args, "--timeout-ms", 0));
            kit.run(true);
            return;
        }

        JFrame frame = new JFrame("KiT Emulator");
    
        kit.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel label = new JLabel();
        kit.add(label, gbc);

        JPanel controlPanel = new JPanel(new GridBagLayout());
        JButton resetButton = new JButton("Reset");  
        resetButton.setFocusable(false);
        resetButton.setBounds(0,0,95,30);  
        resetButton.addActionListener(new ActionListener() {  
            public void actionPerformed(ActionEvent e){  
                kit.reset();            
            }  
        });  
        controlPanel.add(resetButton, gbc);

        JLabel clockLabel = new JLabel();
        gbc.gridy = 1;
        controlPanel.add(clockLabel, gbc);

        JButton loadButton = new JButton("Load");
        loadButton.setFocusable(false);
        loadButton.setBounds(0,0,95,30);  
        loadButton.addActionListener(new ActionListener() {  
            public void actionPerformed(ActionEvent e){  
                JFileChooser chooser= new JFileChooser("/Users/tomlinsonk/projects/6502/6502-software/prgs");

                int choice = chooser.showOpenDialog(null);

                if (choice != JFileChooser.APPROVE_OPTION) return;

                File chosenFile = chooser.getSelectedFile(); 
                // System.out.println(chosenFile.getAbsolutePath());
                kit.startLoad(chosenFile);
            }  
        });  
        gbc.gridy = 5;
        controlPanel.add(loadButton, gbc);

        addSpeedButtons(controlPanel, kit);

        gbc.gridy = 0;
        kit.add(controlPanel, gbc);
        kit.setClockLabel(clockLabel);
        
        frame.setContentPane(kit);

        frame.setSize(1200, 900);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                kit.onExit();
            }
        });

        kit.run();

        // Display update thread
        new Thread(new Runnable() {
			@Override
			public void run() {
                BufferedImage resized = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);
                ImageIcon image;
                Graphics2D g2d;

                while (true) {
                    g2d = resized.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2d.drawImage(kit.getDisplayImage(), 0, 0, 1024, 768, null);
                    g2d.dispose();

                    image = new ImageIcon(resized);
                    label.setIcon(image);

                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } 
            }
		}).start();

        loadDefaultPrg(kit);
    }   
}
