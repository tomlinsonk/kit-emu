public class KiT {

    private final static int RAM_START = 0x0000;
    private final static int RAM_END = 0x6FFF;

    private final static int ROM_START = 0x8000;
    private final static int ROM_END = 0xFFFF;

    private final static int VIA_START = 0x7810;
    private final static int VIA_END = 0x781F;

    




    public static void main(String[] args) {
        Bus bus = new Bus();
        CPU cpu = new CPU(bus);
        RAM ram = new RAM(bus, RAM_START, RAM_END);
        ROM rom = new ROM(bus, ROM_START, ROM_END);
        VIA via = new VIA(bus, VIA_START, VIA_END);

        AddressDecoder addrDecoder = new AddressDecoder();

        addrDecoder.addDevice(ram, RAM_START, RAM_END);
        addrDecoder.addDevice(rom, ROM_START, ROM_END);
        addrDecoder.addDevice(via, VIA_START, VIA_END);

        bus.setAddressDecoder(addrDecoder);
        bus.addInterrupter(via);

        cpu.reset();
        cpu.printStatus();

        int i = 0;
        while (true) {
            cpu.step();
            cpu.printStatus();
            i++;
            if (i > 10) break;
        }

        via.keyPress(0xFF);

        while (true) {
            cpu.step();
            cpu.printStatus();
            i++;
            if (i > 20) break;
        }

        // System.out.println("RAM:");
        // for (int j = 0xFD; j < 0x0120; j++) {
        //     System.out.println(j + ": " + ram.get(j));
        // }
    }   
}
