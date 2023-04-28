import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class KiT extends JPanel {

    private final static int RAM_START = 0x0000;
    private final static int RAM_END = 0x6FFF;

    private final static int ROM_START = 0x9000;
    private final static int ROM_END = 0xFFFF;

    private final static int VIA_START = 0x7810;
    private final static int VIA_END = 0x781F;

    private Bus bus;
    private CPU cpu;
    private RAM ram;
    private ROM rom;
    private VIA via;

    public KiT() {
        KeyListener listener = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (PS2.isExtended(keyCode)) {
                    via.kbByte(PS2.EXTENDED_CODE);
                    kbByteDelay();
                }

                via.kbByte(PS2.getScanCode(keyCode));
			}

			@Override
			public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();

                via.kbByte(PS2.RELEASE_CODE);
                kbByteDelay();

                if (PS2.isExtended(keyCode)) {
                    via.kbByte(PS2.EXTENDED_CODE);
                    kbByteDelay();
                }

                via.kbByte(PS2.getScanCode(keyCode));

			}
		};

		addKeyListener(listener);
		setFocusable(true);


        bus = new Bus();
        cpu = new CPU(bus);
        ram = new RAM(bus, RAM_START, RAM_END);
        rom = new ROM(bus, ROM_START, ROM_END);
        via = new VIA(bus, VIA_START, VIA_END);

        AddressDecoder addrDecoder = new AddressDecoder();

        addrDecoder.addDevice(ram, RAM_START, RAM_END);
        addrDecoder.addDevice(rom, ROM_START, ROM_END);
        addrDecoder.addDevice(via, VIA_START, VIA_END);

        bus.setAddressDecoder(addrDecoder);
        bus.addInterrupter(via);

        cpu.reset();
        cpu.printStatus();

        init();
    }

    private void init() {
        new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				int i = 0;
                long start = System.currentTimeMillis();

                long currTime;
                int prevCycleCount = 0;
                int cycleCount = 0;

                long nsElapsed;
                while (cycleCount < 5_000_000) {
                    currTime = System.nanoTime();

                    cpu.step();

                    cycleCount = cpu.getCycleCount();
                    nsElapsed = 920 * (cycleCount - prevCycleCount);
                    
                    while ((System.nanoTime() - currTime) < nsElapsed) { 
                        continue;
                    }

                    prevCycleCount = cycleCount;

                    // cpu.printStatus();
                    // i++;
                    // if (i > 10) break;


                }

                System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");

                for (int j = 2; j < 6; j++) {
                    for (int k = 0; k < 8; k++) {
                        System.out.print(ram.get(j * 8 + k) + "\t");
                        
                    }
                    System.out.println();
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

    public static void main(String[] args) {
        

        // System.out.println("RAM:");
        // for (int j = 0xFD; j < 0x0120; j++) {
        //     System.out.println(j + ": " + ram.get(j));
        // }

        PS2.getScanCode(KeyEvent.VK_A);
        KiT kit = new KiT();

        JFrame frame = new JFrame("example");
        
        frame.add(kit);
        
        frame.setSize(300,300);
        frame.setLayout(null);
        frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }   
}
