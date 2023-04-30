public class SID implements BusListener {

    private Bus bus;
    private int startAddr;
    private int endAddr;

    SID(Bus bus, int startAddr, int endAddr) {
        this.bus = bus;
        this.startAddr = startAddr;
        this.endAddr = endAddr;
    }

    @Override
    public void activate() {

    }
}
