import java.util.ArrayList;

public class Bus {
    
    private int data;
    private int addr;
    private boolean readBit; 
    private AddressDecoder addrDecoder;
    private ArrayList<Interrupter> interrupters;
    
    public Bus() {
        this.data = 0;
        this.addr = 0;
        this.readBit = true;
        this.interrupters = new ArrayList<Interrupter>();
    }

    public int read(int addr) {
        this.addr = addr;
        this.readBit = true;

        System.out.println("Read " + addr);

        addrDecoder.decode(addr);

        return this.data;
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
