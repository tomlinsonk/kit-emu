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

    AddressDecoder() {
        devices = new ArrayList<>();
    }

    public void addDevice(BusListener device, int startAddr, int endAddr) {
        Device newDevice = new Device(device, startAddr, endAddr);

        for (Device d : devices) {
            assert !newDevice.hasOverlap(d);
        }

        this.devices.add(newDevice);
    }

    public void decode(int addr) {
        for (Device d : devices) {
            if (addr >= d.startAddr && addr <= d.endAddr) {
                d.device.activate();
                return;
            }
        }

        assert false: "Can't decode address $" + Integer.toHexString(addr);
    }

    public int debugRead(int addr) {
        for (Device d : devices) {
            if (addr >= d.startAddr && addr <= d.endAddr) {
                return d.device.debugRead(addr);
            }
        }

        assert false: "Can't decode address $" + Integer.toHexString(addr);
        return -1;
    }

    
}
