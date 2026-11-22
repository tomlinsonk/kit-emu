/**
 * An expansion slot with no card. Reads return the high byte of the address,
 * the last value the CPU drove onto the data bus. Writes do nothing.
 */
public class OpenBus implements BusListener {

    private Bus bus;

    OpenBus(Bus bus) {
        this.bus = bus;
    }

    @Override
    public void activate() {
        if (bus.readBitSet()) {
            bus.setData(bus.getAddr() >> 8);
        }
    }

    @Override
    public int debugRead(int addr) {
        return addr >> 8;
    }
}
