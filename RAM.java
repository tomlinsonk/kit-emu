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
        // System.out.println("RAM activated, bus addr " + bus.getAddr());
        if (bus.readBitSet()) {
            bus.setData(this.data[bus.getAddr() - startAddr]);
        } else {
            this.data[bus.getAddr() - startAddr] = bus.getData();
        }
    }

    public int get(int i) {
        return this.data[i];
    }
}
