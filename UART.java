import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class UART implements BusListener {

    private Bus bus;
    private int startAddr;
    private int endAddr;

    private int readCount;


    private static final int LSR = 5;
    private static final int RBR = 0;

    private static final int DATA_READY = 0b00000001;
    private static final int THR_EMPTY = 0b00100000;

    private int[] registers;


    private AtomicInteger rxData;

    UART(Bus bus, int startAddr, int endAddr) {
        this.bus = bus;
        this.startAddr = startAddr;
        this.endAddr = endAddr;

        registers = new int[8];
        readCount = 0;

        registers[RBR] = 5;
        registers[LSR] = DATA_READY | THR_EMPTY;
    }

    @Override
    public void activate() {
        // System.out.println(Integer.toHexString(bus.getAddr()));

        int addr = bus.getAddr() - startAddr;

        if (bus.readBitSet()) {
            bus.setData(registers[addr]);
            readCount ++;
            // System.out.println("Read " + addr + " " + readCount);
            // try {
            //     Thread.sleep(5);
            // } catch (InterruptedException e) {
            //     // TODO Auto-generated catch block
            //     e.printStackTrace();
            // }
        } else {
            // System.out.println("Write " + bus.getData() + " " + addr);
        }
    }
}
