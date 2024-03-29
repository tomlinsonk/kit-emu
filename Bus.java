import java.util.ArrayList;

public class Bus {
    
    private int data;
    private int addr;
    private boolean readBit; 
    private boolean resetLine;
    private AddressDecoder addrDecoder;
    private ArrayList<Interrupter> interrupters;
    
    public Bus() {
        this.data = 0;
        this.addr = 0;
        this.readBit = true;
        this.interrupters = new ArrayList<Interrupter>();
        this.resetLine = false;
    }

    public int read(int addr) {
        this.addr = addr;
        this.readBit = true;

        // System.out.println("Read " + addr);

        addrDecoder.decode(addr);

        return this.data;
    }

    /**
     * Perform a read without altering the state of the bus or activating side-effects
     * @param addr
     */
    public int debugRead(int addr) {
        return addrDecoder.debugRead(addr);
    }

    public void write(int addr, int data) {
        this.addr = addr;
        this.data = data;
        this.readBit = false;

        addrDecoder.decode(addr);
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public boolean getIRQ() {
        for (Interrupter interrupter : interrupters) {
            if (interrupter.hasIRQ()) {
                return true;
            }
        }

        return false;
    }

    public void addInterrupter(Interrupter x) {
        this.interrupters.add(x);
    }

    public int getAddr() {
        return addr;
    }

    public void setAddr(int address) {
        this.addr = address;
    }

    public boolean readBitSet() {
        return this.readBit;
    }

    public void setAddressDecoder(AddressDecoder addrDecoder) {
        this.addrDecoder = addrDecoder;
    }
}
