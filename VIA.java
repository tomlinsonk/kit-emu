public class VIA implements Interrupter, BusListener {

    // VIA registers (address relative to startAddr)
    private static final int B = 0;                 
    private static final int A = 1; 
    private static final int DDRB = 2; 
    private static final int DDRA = 3; 
    private static final int T1C_L = 4; 
    private static final int T1C_H = 5; 
    private static final int T1L_L = 6; 
    private static final int T1L_H = 7; 
    private static final int T2C_L = 8; 
    private static final int T2C_H = 9; 
    private static final int SR = 10; 
    private static final int ACR = 11; 
    private static final int PCR = 12; 
    private static final int IFR = 13; 
    private static final int IER = 14; 
    private static final int A_2 = 15; 

    private static final String[] REGISTER_NAMES = {"B", "A", "DDRB", "DDRA", "T1C_L", "T1C_H", "T1L_L", "T1L_H", "T2C_L", "T2C_H", "SR", "ACR", "PCR", "IFR", "IER", "A_2"};

    // VIA Interrupt Bits
    private static final int CA1_INT = 0b00000010;
    private static final int ANY_INT = 0b10000000;
    private static final int INT_ENABLE = 0b10000000;


    private int[] registers;

    private int startAddr;
    private int endAddr;

    private int portAVal;
    private int portBVal;

    private Bus bus;

    public VIA(Bus bus, int startAddr, int endAddr) {
        this.startAddr = startAddr;
        this.endAddr = endAddr;
        this.bus = bus;
        this.registers = new int[16];
    }

    @Override
    public boolean hasIRQ() {
        return (registers[IFR] & ANY_INT) != 0;
    }

    public void kbByte(int val) {
        System.out.println("VIA kb byte:" + val);
        portAVal = val;
        
        // If CA1 interrupt is enabled
        if ((registers[IER] & CA1_INT) != 0) {
            // Set the interrupt flags
            registers[IFR] = registers[IFR] | ANY_INT | CA1_INT;
        }
    }

    @Override
    public void activate() {

        int registerAddr = bus.getAddr() - startAddr;
        clearInterrupts(registerAddr, bus.readBitSet());

        if (bus.readBitSet()) {
            readPorts();
            bus.setData(this.registers[registerAddr]);
        } else {
            int val = bus.getData();

            if (registerAddr == A) {
                writePortA(val);
            } else if (registerAddr == B) {
                writePortB(val);
            } else {
                this.registers[registerAddr] = val;
            }
        }
    }

    /**
     * Read the values in port A and port B into the registers,
     * but only bits set as input.
     */
    private void readPorts() {

        // Read bits sets as input from ports into registers
        registers[A] = (portAVal & (~registers[DDRA])) | (registers[A] & registers[DDRA]);
        registers[B] = (portBVal & (~registers[DDRB])) | (registers[B] & registers[DDRB]);

        System.out.println("read ports, A = " + registers[A]);

    }

    private void writePortA(int val) {
        registers[A] = (registers[A] & (~registers[DDRA])) | (val & registers[DDRA]);
        portAVal = (portAVal & (~registers[DDRA])) | (val & registers[DDRA]);
    }

    private void writePortB(int val) {
        registers[B] = (registers[B] & (~registers[DDRB])) | (val & registers[DDRB]);
        portBVal = (portBVal & (~registers[DDRB])) | (val & registers[DDRB]);
    }

    public void printRegisters() {
        for (int i = 0; i < registers.length; i++) {
            System.out.println(String.format("%5s", REGISTER_NAMES[i]) + ": " + registers[i]);
        }
    }

    /**
     * Clear interrupts based on register read/write.
     * Note: doesn't implement CA2/CB2 independent interrupt (see VIA datasheet p. 13)
     * @param registerAddr
     * @param isRead
     */
    private void clearInterrupts(int registerAddr, boolean isRead) {
        System.out.println("clearInterrupts" +  registerAddr + ", " + isRead);
        if (registerAddr == A) {
            registers[IFR] &= 0b11111100;
        } else if (registerAddr == B) {
            registers[IFR] &= 0b11100111;
        } else if (registerAddr == SR) {
            registers[IFR] &= 0b11111011;
        } else if ((registerAddr == T2C_L && isRead) || (registerAddr == T2C_H && !isRead)) {
            registers[IFR] &= 0b11011111;
        } else if ((registerAddr == T1L_L && isRead) || (registerAddr == T1L_H && !isRead)) {
            registers[IFR] &= 0b10111111;
        }

        if ((registers[IFR] & 0b01111111) == 0) {
            registers[IFR] = 0;
        }
    }

    public static void main(String[] args) {
        VIA via = new VIA(null, 0, 16);
        via.printRegisters();

        via.registers[IER] = CA1_INT;

        via.readPorts();
        // via.activate();

        via.printRegisters();

    }
    
}
