import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ROM implements BusListener {
    private Bus bus;
    private int startAddr;

    private int[] data;

    ROM(Bus bus, int startAddr, int endAddr) {
        this.bus = bus;
        this.startAddr = startAddr;

        this.data = new int[endAddr - startAddr + 1];

        byte[] romData;
        try {
            romData = Files.readAllBytes(Paths.get("/Users/tomlinsonk/projects/6502/6502-software/roms/emu-test/emu-test.bin"));
            for (int i = 0; i < romData.length; i++) {
                this.data[i] = romData[i] & 0xFF;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
    }

    public void activate() {
        // System.out.println("ROM activated, bus addr " + bus.getAddr());
        bus.setData(this.data[bus.getAddr() - startAddr]);
    }
}
