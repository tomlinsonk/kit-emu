public class KiT {

    final static int RAM_START = 0x0000;
    final static int RAM_END = 0x6FFF;

    final static int ROM_START = 0x8000;
    final static int ROM_END = 0xFFFF;
    




    public static void main(String[] args) {
        Bus bus = new Bus();
        CPU cpu = new CPU(bus);
        RAM ram = new RAM(bus, RAM_START, RAM_END);
        ROM rom = new ROM(bus, ROM_START, ROM_END);

        AddressDecoder addrDecoder = new AddressDecoder();

        addrDecoder.addDevice(ram, RAM_START, RAM_END);
        addrDecoder.addDevice(rom, ROM_START, ROM_END);

        bus.setAddressDecoder(addrDecoder);

        cpu.reset();
        cpu.printStatus();

        int i = 0;
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
