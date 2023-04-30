import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SSD implements BusListener {

    private Bus bus;
    private VIA via;
    private int startAddr;
    private int endAddr;

    private int[] data;

    public SSD(Bus bus, VIA via, int startAddr, int endAddr) {
        this.bus = bus;
        this.via = via;
        this.startAddr = startAddr;
        this.endAddr = endAddr;


        this.data = new int[1 << 18];

        byte[] ssdData;
        try {
            // romData = Files.readAllBytes(Paths.get("/Users/tomlinsonk/projects/6502/6502-software/roms/emu-test/emu-test.bin"));
            ssdData = Files.readAllBytes(Paths.get("/Users/tomlinsonk/projects/6502/kit-emu/ssd.bin"));

            for (int i = 0; i < ssdData.length; i++) {
                this.data[i] = ssdData[i] & 0xFF;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void activate() {

        int ssdAddr = (bus.getAddr() - startAddr) | (via.getPortB() << 12);

        if (bus.readBitSet()) {
            bus.setData(this.data[ssdAddr]);
        }

    }
}
