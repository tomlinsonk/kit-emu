import java.util.ArrayList;

public class AddressDecoder {

    class Device {
        BusListener device;
        int startAddr;
        int endAddr;

        Device(BusListener device, int startAddr, int endAddr) {
            this.device = device;
            this.startAddr = startAddr;
            this.endAddr = endAddr; 
        }

        boolean hasOverlap(Device other) {
            return ((this.startAddr >= other.startAddr) && (this.startAddr <= other.endAddr)) || ((other.startAddr >= this.startAddr) && (other.startAddr <= this.endAddr));
        }
    }

    private ArrayList<Device> devices;
    private final BusListener[] addressMap = new BusListener[0x10000];

    AddressDecoder() {
        devices = new ArrayList<>();
    }

    public void addDevice(BusListener device, int startAddr, int endAddr) {
        Device newDevice = new Device(device, startAddr, endAddr);

        for (Device d : devices) {
            assert !newDevice.hasOverlap(d);
        }

        this.devices.add(newDevice);
        for (int addr = Math.max(0, startAddr); addr <= Math.min(0xFFFF, endAddr); addr++) {
            if (addressMap[addr] == null) {
                addressMap[addr] = device;
            }
        }
    }

    public void decode(int addr) {
        BusListener device = addr >= 0 && addr < addressMap.length ? addressMap[addr] : null;
        if (device != null) {
            device.activate();
            return;
        }

        assert false: "Can't decode address $" + Integer.toHexString(addr);
    }

    public int debugRead(int addr) {
        BusListener device = addr >= 0 && addr < addressMap.length ? addressMap[addr] : null;
        if (device != null) {
            return device.debugRead(addr);
        }

        assert false: "Can't decode address $" + Integer.toHexString(addr);
        return -1;
    }

    
}
