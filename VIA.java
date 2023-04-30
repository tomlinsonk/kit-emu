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
    private static final int T1_INT = 0b01000000;
    private static final int T2_INT = 0b00100000;
    private static final int ANY_INT = 0b10000000;
    private static final int INT_ENABLE = 0b10000000;

    // VIA ACR bits
    private static final int T1_CONTINUOUS = 0b01000000;

    private int[] registers;

    private int startAddr;
    private int endAddr;

    private int portAVal;
    private int portBVal;

    private long cycleCount;
    private int t1Counter;
    private int t2Counter;
    private boolean t1Counting;
    private boolean t2Counting;

    private Bus bus;

    public VIA(Bus bus, int startAddr, int endAddr) {
        this.startAddr = startAddr;
        this.endAddr = endAddr;
        this.bus = bus;
        this.registers = new int[16];
        this.cycleCount = 0; 
        this.t1Counter = 0;
        this.t2Counter = 0;
        this.t1Counting = false;
        this.t2Counting = false;
    }

    @Override
    public boolean hasIRQ() {
        return (registers[IFR] & ANY_INT) != 0;
    }

    public void kbByte(int val) {
        // System.out.println("VIA kb byte:" + val);
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
            updateRegisters();
            bus.setData(this.registers[registerAddr]);
        } else {
            int val = bus.getData();

            if (registerAddr == A) {
                writePortA(val);
            } else if (registerAddr == B) {
                writePortB(val);
            } else if (registerAddr == T1C_L) {
                registers[T1L_L] = val;
                registers[T1C_L] = val;
            } else if (registerAddr == T1C_H) {
                registers[T1L_H] = val;
                registers[T1C_H] = val;
                t1Counting = true;
                t1Counter = (registers[T1L_H] << 8) | registers[T1L_L];
            } else if (registerAddr == T2C_H) {
                registers[T2C_H] = val;
                t2Counting = true;
                t2Counter = (registers[T2C_H] << 8) | registers[T2C_L];
            } else {
                registers[registerAddr] = val;
            }
        }
    }

    public void updateCycleCount(int newCycles) {
        cycleCount += newCycles;

        if (t1Counting) {
            t1Counter -= newCycles;
            if (t1Counter <= 0) {
                if ((registers[ACR] & T1_CONTINUOUS) != 0) {
                    t1Counter = (registers[T1L_H] << 8) | registers[T1L_L];
                } else {
                    t1Counting = false;
                    t1Counter = 0;
                }

                if ((registers[IER] & T1_INT) != 0) {
                    registers[IFR] = registers[IFR] | ANY_INT | T1_INT;
                }
            }
        }

        if (t2Counting) {
            t2Counter -= newCycles;
            if (t2Counter <= 0) {
                if ((registers[IER] & T2_INT) != 0) {
                    registers[IFR] = registers[IFR] | ANY_INT | T2_INT;
                }
                t2Counting = false;
                t2Counter = 0;
            }
        }
    }

    /**
     * Read the values in port A and port B into the registers,
     * but only bits set as input. Also updates timer counters
     */
    private void updateRegisters() {

        // Read bits sets as input from ports into registers
        registers[A] = (portAVal & (~registers[DDRA])) | (registers[A] & registers[DDRA]);
        registers[B] = (portBVal & (~registers[DDRB])) | (registers[B] & registers[DDRB]);

        registers[T1C_L] = t1Counter & 0xFF;
        registers[T1C_H] = (t1Counter >> 8) & 0xFF;

        registers[T2C_L] = t2Counter & 0xFF;
        registers[T2C_H] = (t2Counter >> 8) & 0xFF;
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
        // System.out.println("clearInterrupts" +  registerAddr + ", " + isRead);
        if (registerAddr == A) {
            registers[IFR] &= 0b11111100;
        } else if (registerAddr == B) {
            registers[IFR] &= 0b11100111;
        } else if (registerAddr == SR) {
            registers[IFR] &= 0b11111011;
        } else if ((registerAddr == T2C_L && isRead) || (registerAddr == T2C_H && !isRead)) {
            registers[IFR] &= 0b11011111;
        } else if ((registerAddr == T1C_L && isRead) || (registerAddr == T1C_H && !isRead) || (registerAddr == T1L_H && !isRead)) {
            registers[IFR] &= 0b10111111;
        }

        if ((registers[IFR] & 0b01111111) == 0) {
            registers[IFR] = 0;
        }
    }

    public int getPortA() {
        return registers[A];
    }

    public int getPortB() {
        return registers[B];
    }

    public static void main(String[] args) {
        VIA via = new VIA(null, 0, 16);
        via.printRegisters();

        via.registers[IER] = CA1_INT;

        // via.readPorts();
        // via.activate();

        via.printRegisters();

    }
    
}
