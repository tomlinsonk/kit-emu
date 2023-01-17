public class VIA implements Interrupter, BusListener {

    private boolean IRQ;
    private int startAddr;
    private int endAddr;
    private int portAVal;
    private int portBVal;
    private Bus bus;


    public VIA(Bus bus, int startAddr, int endAddr) {
        IRQ = false;
        this.startAddr = startAddr;
        this.endAddr = endAddr;
        this.bus = bus;
    }

    @Override
    public boolean hasIRQ() {
        return IRQ;
    }

    public void keyPress(int scanCode) {
        portAVal = scanCode;
        IRQ = true;
    }

    @Override
    public void activate() {
        IRQ = false;
    }
    
}
