public class UART implements BusListener {

    private Bus bus;
    private int startAddr;
    private int endAddr;

    UART(Bus bus, int startAddr, int endAddr) {
        this.bus = bus;
        this.startAddr = startAddr;
        this.endAddr = endAddr;
    }

    @Override
    public void activate() {

    }
}
