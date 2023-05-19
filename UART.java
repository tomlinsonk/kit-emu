import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    private int[] loadData;
    private int loadIndex;

    UART(Bus bus, int startAddr, int endAddr) {
        this.bus = bus;
        this.startAddr = startAddr;
        this.endAddr = endAddr;

        registers = new int[8];
        readCount = 0;

        registers[LSR] = DATA_READY | THR_EMPTY;

        loadIndex = 0;
        loadData = null;
    }

    @Override
    public void activate() {
        // System.out.println(Integer.toHexString(bus.getAddr()));

        int addr = bus.getAddr() & 0x0F;

        if (bus.readBitSet()) {
            if (addr == RBR && loadData != null && loadIndex < loadData.length) {
                registers[RBR] = loadData[loadIndex];
                // System.out.println(loadData[loadIndex]);
                loadIndex++;
            } 

            bus.setData(registers[addr]);
        } else {
            // System.out.println("Write " + bus.getData() + " " + addr);
        }
    }


    public void loadPrg(File f) {
        loadIndex = 0;

        try {
            byte[] fileBytes = Files.readAllBytes(f.toPath());
            loadData = new int[fileBytes.length + 8];

            loadData[0] = 0x00;
            loadData[1] = 0xFF;

            int sum1 = 0;
            int sum2 = 0;

            for (int i = 10; i < 8 + fileBytes.length; i++) {
                loadData[i] = fileBytes[i - 8] & 0xFF;
                sum1 = (sum1 + loadData[i]) % 256;
                sum2 = (sum2 + sum1) % 256;

            }

            int fileSize = fileBytes.length - 2;

            // filesize
            loadData[2] = fileSize & 0xFF;
            loadData[3] = fileSize >> 8;

            // checksum
            loadData[4] = sum1;
            loadData[5] = sum2;

            // start addr
            loadData[6] = fileBytes[0] & 0xFF;
            loadData[7] = fileBytes[1] & 0xFF;

            // load addr
            loadData[8] = fileBytes[0] & 0xFF;
            loadData[9] = fileBytes[1] & 0xFF;


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
