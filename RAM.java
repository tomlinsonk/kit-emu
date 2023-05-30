public class RAM implements BusListener {
    private Bus bus;
    private int startAddr;
    private int endAddr;
    private int[] data;

    RAM(Bus bus, int startAddr, int endAddr) {
        this.bus = bus;
        this.startAddr = startAddr;
        this.endAddr = endAddr;

        this.data = new int[endAddr - startAddr + 1];
    }

    public void activate() {
        int addr = bus.getAddr() - startAddr;
        // System.out.println("RAM activated, bus addr " + bus.getAddr());
        if (bus.readBitSet()) {
            bus.setData(this.data[addr]);

            // if (addr >= 0x1b05) {
            //     System.out.println("RAM READ " + Integer.toHexString(this.data[addr]) + " AT " + Integer.toHexString(addr));
            // }
        } else {
            this.data[addr] = bus.getData();
            // if (addr >= 0x1b05) {
            //     System.out.println("RAM WRITE " + Integer.toHexString(this.data[addr]) + " AT " + Integer.toHexString(addr));
            // }
        }
    }

    public int get(int i) {
        return this.data[i];
    }
}
