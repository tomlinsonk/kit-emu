public class Bus {
    
    private int data;
    private int addr;
    private boolean readBit; 
    private AddressDecoder addrDecoder;
    
    public Bus() {
        this.data = 0;
        this.addr = 0;
        this.readBit = true;
    }

    public int read(int addr) {
        this.addr = addr;
        this.readBit = true;

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
