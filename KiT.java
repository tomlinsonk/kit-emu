import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

public class KiT extends JPanel {

    private final static int RAM_START = 0x0000;
    private final static int RAM_END = 0x5FFF;

    private final static int VRAM_START = 0x6000;
    private final static int VRAM_END = 0x77FF;

    private final static int VIA1_START = 0x7810;
    private final static int VIA1_END = 0x781F;

    private final static int VIA2_START = 0x7900;
    private final static int VIA2_END = 0x790F;

    private final static int UART_START = 0x7A00;
    private final static int UART_END = 0x7A0F;

    private final static int SID_START = 0x7E00;
    private final static int SID_END = 0x7E0F;

    private final static int SSD_START = 0x8000;
    private final static int SSD_END = 0x8FFF;

    private final static int ROM_HIDDEN_START = 0x8000;
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


    private Graphics graphics;

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

        graphics = new Graphics(bus, VRAM_START, VRAM_END);

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

        cpu.reset();
        // cpu.printStatus();

        run();
    }

    private class PS2KeyListender implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (PS2.isExtended(keyCode)) {
                via1.kbByte(PS2.EXTENDED_CODE);
                kbByteDelay();
            }

            via1.kbByte(PS2.getScanCode(keyCode));
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int keyCode = e.getKeyCode();

            via1.kbByte(PS2.RELEASE_CODE);
            kbByteDelay();

            if (PS2.isExtended(keyCode)) {
                via1.kbByte(PS2.EXTENDED_CODE);
                kbByteDelay();
            }

            via1.kbByte(PS2.getScanCode(keyCode));

        }
    }

    private void run() {
        new Thread(new Runnable() {
			@Override
			public void run() {
				long currTime;
                long prevCycleCount = 0;
                long cycleCount = 0;
                int newCycles;

                long checkpointTime = 0;
                long checkpointCycles = 0;

                long nsElapsed;
                while (true) {
                    currTime = System.nanoTime();

                    cpu.step();

                    cycleCount = cpu.getCycleCount();
                    newCycles = (int)(cycleCount - prevCycleCount);
                    nsElapsed = 495 * newCycles;
                    
                    while ((System.nanoTime() - currTime) < nsElapsed) { 
                        continue;
                    }

                    prevCycleCount = cycleCount;

                    via1.updateCycleCount(newCycles);
                    via2.updateCycleCount(newCycles);
                    // cpu.printStatus();
                    // i++;
                    // if (i > 10) break;

                    if (currTime - checkpointTime > 2_000_000_000) {
                        System.out.println(((cycleCount - checkpointCycles) / 2_000_000.0) + " MHz");
                        checkpointTime = currTime;
                        checkpointCycles = cycleCount;
                    }
                }
			}
		}).start();
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

    public static void main(String[] args) {
        

        // System.out.println("RAM:");
        // for (int j = 0xFD; j < 0x0120; j++) {
        //     System.out.println(j + ": " + ram.get(j));
        // }

        PS2.getScanCode(KeyEvent.VK_A);
        KiT kit = new KiT();


        JFrame frame = new JFrame("KiT Emulator");
        

        kit.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel label = new JLabel();
        kit.add(label, gbc);

        frame.setContentPane(kit);

        frame.setSize(1200, 900);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


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
    }   
}
