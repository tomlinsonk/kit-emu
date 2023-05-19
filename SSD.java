import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SSD implements BusListener {

    private Bus bus;
    private VIA via;
    private int startAddr;
    private int endAddr;

    private int[] data;

    private int byteProgramIdx;
    private int sectorEraseIdx;

    private static final int[] BYTE_PROGRAM_ADDRS = {0x5555, 0x2AAA, 0x5555};
    private static final int[] BYTE_PROGRAM_DATA = {0xAA, 0x55, 0xA0};

    private static final int[] SECTOR_ERASE_ADDRS = {0x5555, 0x2AAA, 0x5555, 0x5555, 0x2AAA};
    private static final int[] SECTOR_ERASE_DATA = {0xAA, 0x55, 0x80, 0xAA, 0x55, 0x30};


    public SSD(Bus bus, VIA via, int startAddr, int endAddr) {
        this.bus = bus;
        this.via = via;
        this.startAddr = startAddr;
        this.endAddr = endAddr;

        this.byteProgramIdx = 0;
        this.sectorEraseIdx = 0;


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
            // System.out.println("read " + Integer.toHexString(ssdAddr) + ":" + this.data[ssdAddr]);

        } else {

            int busData = bus.getData();
            // System.out.println("w " + Integer.toHexString(busData) + " to " + Integer.toHexString(ssdAddr));

            if (byteProgramIdx == BYTE_PROGRAM_ADDRS.length) {
                // System.out.println("WRITE " + Integer.toHexString(busData) + " to " + Integer.toHexString(ssdAddr));

                this.data[ssdAddr] = busData;
                byteProgramIdx = 0;
                sectorEraseIdx = 0;
            } else if (sectorEraseIdx == SECTOR_ERASE_ADDRS.length) {
                if (busData == SECTOR_ERASE_DATA[sectorEraseIdx]) {
                    System.out.println("ERASE " + Integer.toHexString(ssdAddr));

                    int eraseAddr = ssdAddr & 0b111111000000000000;
                    for (int i = eraseAddr; i < eraseAddr + 4096; i++) {
                        this.data[i] = 0xFF;
                    }
                    sectorEraseIdx = 0;
                    byteProgramIdx = 0;
                } else {
                    sectorEraseIdx = 0;
                }
            } else {
                if (ssdAddr == BYTE_PROGRAM_ADDRS[byteProgramIdx] && busData == BYTE_PROGRAM_DATA[byteProgramIdx]) {
                    byteProgramIdx++;
                    // System.out.println("write i = " + byteProgramIdx);

                } else {
                    byteProgramIdx = 0;
                    // System.out.println("write i = " + byteProgramIdx);

                }

                if (ssdAddr == SECTOR_ERASE_ADDRS[sectorEraseIdx] && busData == SECTOR_ERASE_DATA[sectorEraseIdx]) {
                    sectorEraseIdx++;
                    System.out.println("erase i = " + sectorEraseIdx);
                } else {
                    sectorEraseIdx = 0;
                    System.out.println("erase i = " + sectorEraseIdx);

                }
            }
        }
    }

    public void writeFile() {
        byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte)data[i];
        }

        try (FileOutputStream stream = new FileOutputStream("/Users/tomlinsonk/projects/6502/kit-emu/ssd.bin")) {
            stream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
