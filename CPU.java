
public class CPU {

    enum AddressingMode {
        ACCUMULATOR(0),
        ABSOLUTE(2),
        ABSOLUTE_X(2),
        ABSOLUTE_Y(2),
        IMMEDIATE(1),
        IMPLIED(0),
        INDIRECT(2),
        X_INDIRECT(1),
        INDIRECT_Y(1),
        RELATIVE(1),
        ZEROPAGE(1),
        ZEROPAGE_X(1),
        ZEROPAGE_Y(1),
        ZEROPAGE_INDIRECT(2);
        

        private final int operandBytes;

        AddressingMode(int operandBytes) {
            this.operandBytes = operandBytes;
        }

        public int operandBytes() { return this.operandBytes; }
    }

    abstract class Instruction {
        int opcode;
        int bytes;
        int cycles;
        String mnemonic;
        AddressingMode addrMode;

        Instruction(String mnemonic, AddressingMode addrMode, int opcode, int bytes, int cycles) {
            this.opcode = opcode;
            this.bytes = bytes;
            this.cycles = cycles;
            this.mnemonic = mnemonic;
            this.addrMode = addrMode;
        }

        public abstract void exec();
    }

    /*
     * Load instructions
     */
    abstract class LDA extends Instruction {
        LDA(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles); 
        }

        public void exec() {
            A = getLoadVal(this.addrMode);
            updateNZ(A);
            incPC();
        }
    }

    class LDAImmediate extends LDA {
        LDAImmediate() {
            super("lda", AddressingMode.IMMEDIATE, 0xA9, 2, 2);
        }
    }

    class LDAZeropage extends LDA {
        LDAZeropage() {
            super("lda", AddressingMode.ZEROPAGE, 0xA5, 2, 3);
        }
    }

    class LDAZeropageX extends LDA {
        LDAZeropageX() {
            super("lda", AddressingMode.ZEROPAGE_X, 0xB5, 2, 4);
        }
    }

    class LDAAbsolute extends LDA {
        LDAAbsolute() {
            super("lda", AddressingMode.ABSOLUTE, 0xAD, 3, 4);
        }
    }

    class LDAAbsoluteX extends LDA {
        LDAAbsoluteX() {
            super("lda", AddressingMode.ABSOLUTE_X, 0xBD, 3, 4);
        }
    }

    class LDAAbsoluteY extends LDA {
        LDAAbsoluteY() {
            super("lda", AddressingMode.ABSOLUTE_Y, 0xB9, 3, 4);
        }
    }

    class LDAXIndirect extends LDA {
        LDAXIndirect() {
            super("lda", AddressingMode.X_INDIRECT, 0xA1, 2, 6);
        }
    }

    class LDAIndirectY extends LDA {
        LDAIndirectY() {
            super("lda", AddressingMode.INDIRECT_Y, 0xB1, 2, 5);
        }
    }


    abstract class LDX extends Instruction {
        LDX(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles); 
        }

        @Override
        public void exec() {
            X = getLoadVal(this.addrMode);
            updateNZ(X);
            incPC();
        }
    }

    class LDXImmediate extends LDX {
        LDXImmediate() {
            super("ldx", AddressingMode.IMMEDIATE, 0xA2, 2, 2);
        }
    }

    class LDXZeropage extends LDX {
        LDXZeropage() {
            super("ldx", AddressingMode.ZEROPAGE, 0xA6, 2, 3);
        }
    }

    class LDXZeropageY extends LDX {
        LDXZeropageY() {
            super("ldx", AddressingMode.ZEROPAGE_Y, 0xB6, 2, 4);
        }
    }

    class LDXAbsolute extends LDX {
        LDXAbsolute() {
            super("ldx", AddressingMode.ABSOLUTE, 0xAE, 3, 4);
        }
    }

    class LDXAbsoluteY extends LDX {
        LDXAbsoluteY() {
            super("ldx", AddressingMode.ABSOLUTE_Y, 0xBE, 3, 4);
        }
    }


    abstract class LDY extends Instruction {
        LDY(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles); 
        }

        public void exec() {
            Y = getLoadVal(this.addrMode);
            updateNZ(Y);
            incPC();
        }
    }

    class LDYImmediate extends LDY {
        LDYImmediate() {
            super("ldy", AddressingMode.IMMEDIATE, 0xA0, 2, 2);
        }
    }

    class LDYZeropage extends LDY {
        LDYZeropage() {
            super("ldy", AddressingMode.ZEROPAGE, 0xA4, 2, 3);
        }
    }

    class LDYZeropageX extends LDY {
        LDYZeropageX() {
            super("ldy", AddressingMode.ZEROPAGE_X, 0xB4, 2, 4);
        }
    }

    class LDYAbsolute extends LDY {
        LDYAbsolute() {
            super("ldy", AddressingMode.ABSOLUTE, 0xAC, 3, 4);
        }
    }

    class LDYAbsoluteX extends LDY {
        LDYAbsoluteX() {
            super("ldy", AddressingMode.ABSOLUTE_X, 0xBC, 3, 4);
        }
    }


    /*
     * Store instructions
     */
    abstract class STA extends Instruction {
        STA(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        @Override
        public void exec() {
            bus.write(getOperandAddress(this.addrMode), A);
            incPC();
        }
        
    }

    class STAZeropage extends STA {
        STAZeropage() {
            super("sta", AddressingMode.ZEROPAGE, 0x85, 2, 3);
        }
    }

    class STAZeropageX extends STA {
        STAZeropageX() {
            super("sta", AddressingMode.ZEROPAGE_X, 0x95, 2, 4);
        }
    }

    class STAAbsolute extends STA {
        STAAbsolute() {
            super("sta", AddressingMode.ABSOLUTE, 0x8D, 3, 4);
        }
    }

    class STAAbsoluteX extends STA {
        STAAbsoluteX() {
            super("sta", AddressingMode.ABSOLUTE_X, 0x9D, 3, 5);
        }
    }

    class STAAbsoluteY extends STA {
        STAAbsoluteY() {
            super("sta", AddressingMode.ABSOLUTE_Y, 0x99, 3, 5);
        }
    }

    class STAXIndirect extends STA {
        STAXIndirect() {
            super("sta", AddressingMode.X_INDIRECT, 0x81, 2, 6);
        }
    }

    class STAIndirectY extends STA {
        STAIndirectY() {
            super("sta", AddressingMode.INDIRECT_Y, 0x91, 2, 6);
        }
    }



    abstract class STX extends Instruction {
        STX(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        @Override
        public void exec() {
            bus.write(getOperandAddress(this.addrMode), X);
            incPC();
        } 
    }

    class STXZeropage extends STX {
        STXZeropage() {
            super("stx", AddressingMode.ZEROPAGE, 0x86, 2, 3);
        }
    }

    class STXZeropageY extends STX {
        STXZeropageY() {
            super("stx", AddressingMode.ZEROPAGE_Y, 0x96, 2, 4);
        }
    }

    class STXAbsolute extends STX {
        STXAbsolute() {
            super("stx", AddressingMode.ABSOLUTE, 0x8E, 3, 4);
        }
    }


    abstract class STY extends Instruction {
        STY(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        @Override
        public void exec() {
            bus.write(getOperandAddress(this.addrMode), Y);
            incPC();
        } 
    }

    class STYZeropage extends STY {
        STYZeropage() {
            super("sty", AddressingMode.ZEROPAGE, 0x84, 2, 3);
        }
    }

    class STYZeropageX extends STY {
        STYZeropageX() {
            super("sty", AddressingMode.ZEROPAGE_X, 0x94, 2, 4);
        }
    }

    class STYAbsolute extends STY {
        STYAbsolute() {
            super("sty", AddressingMode.ABSOLUTE, 0x8C, 3, 4);
        }
    }

    abstract class STZ extends Instruction {
        STZ(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        @Override
        public void exec() {
            bus.write(getOperandAddress(this.addrMode), 0);
            incPC();
        }  
    }

    class STZZeropage extends STZ {
        STZZeropage() {
            super("stz", AddressingMode.ZEROPAGE, 0x64, 2, 4);
        }
    }

    class STZZeropageX extends STZ {
        STZZeropageX() {
            super("stz", AddressingMode.ZEROPAGE_X, 0x74, 2, 5);
        }
    }

    class STZAbsolute extends STZ {
        STZAbsolute() {
            super("stz", AddressingMode.ABSOLUTE, 0x9c, 3, 5);
        }
    }

    class STZAbsoluteX extends STZ {
        STZAbsoluteX() {
            super("stz", AddressingMode.ABSOLUTE_X, 0x9e, 3, 6);
        }
    }



    /*
     * Transfer instructions
     */
    class TAX extends Instruction {
        TAX() {
            super("tax", AddressingMode.IMPLIED, 0xAA, 1, 2);
        }

        @Override
        public void exec() {
            X = A;
            updateNZ(X);
            incPC();    
        }
    }
    class TAY extends Instruction {
        TAY() {
            super("tay", AddressingMode.IMPLIED, 0xA8, 1, 2);
        }

        @Override
        public void exec() {
            Y = A;
            updateNZ(Y);
            incPC();    
        }
    }

    class TSX extends Instruction {
        TSX() {
            super("tsx", AddressingMode.IMPLIED, 0xBA, 1, 2);
        }

        @Override
        public void exec() {
            X = S;
            updateNZ(X);
            incPC();    
        }
    }

    class TXA extends Instruction {
        TXA() {
            super("txa", AddressingMode.IMPLIED, 0x8A, 1, 2);
        }

        @Override
        public void exec() {
            A = X;
            updateNZ(A);
            incPC();    
        }
    }

    class TXS extends Instruction {
        TXS() {
            super("txs", AddressingMode.IMPLIED, 0x9A, 1, 2);
        }

        @Override
        public void exec() {
            S = X;
            updateNZ(S);
            incPC();    
        }
    }

    class TYA extends Instruction {
        TYA() {
            super("tya", AddressingMode.IMPLIED, 0x98, 1, 2);
        }

        @Override
        public void exec() {
            A = Y;
            updateNZ(A);
            incPC();    
        }
    }

    /*
     * Stack instructions
     */

     private int pop() {
        S = (S + 1) & 0xFF;
        int val = bus.read(0x0100 + S);
        return val;
    }

    private int popAndIncPC() {
        S = (S + 1) & 0xFF;
        int val = bus.read(0x0100 + S);
        updateNZ(val);
        incPC();
        return val;
    }

    private void push(int val) {
        bus.write(0x0100 + S, val);
        S = (S - 1) & 0xFF;
    }

    private void pushAndIncPC(int val) {
        bus.write(0x0100 + S, val);
        S = (S - 1) & 0xFF;
        incPC();
    }

    class PHA extends Instruction {
        PHA() {
           super("pha", AddressingMode.IMPLIED, 0x48, 1, 3); 
        }

        @Override
        public void exec() {
            pushAndIncPC(A);
        }
    }

    class PLA extends Instruction {
        PLA() {
           super("pla", AddressingMode.IMPLIED, 0x68, 1, 4); 
        }

        @Override
        public void exec() {
            A = popAndIncPC();
        }
    }

    class PHX extends Instruction {
        PHX() {
           super("phx", AddressingMode.IMPLIED, 0xda, 1, 3); 
        }

        @Override
        public void exec() {
            pushAndIncPC(X);
        }
    }

    class PLX extends Instruction {
        PLX() {
           super("plx", AddressingMode.IMPLIED, 0xfa, 1, 4); 
        }

        @Override
        public void exec() {
            X = popAndIncPC();
        }
    }

    class PHY extends Instruction {
        PHY() {
           super("phy", AddressingMode.IMPLIED, 0x5a, 1, 3); 
        }

        @Override
        public void exec() {
            pushAndIncPC(Y);
        }
    }

    class PLY extends Instruction {
        PLY() {
           super("ply", AddressingMode.IMPLIED, 0x7a, 1, 4); 
        }

        @Override
        public void exec() {
            Y = popAndIncPC();
        }
    }

    class PHP extends Instruction {
        PHP() {
           super("php", AddressingMode.IMPLIED, 0x08, 1, 3); 
        }

        @Override
        public void exec() {
            pushAndIncPC(getP() | 0b00110000);
        }
    }

    class PLP extends Instruction {
        PLP() {
           super("plp", AddressingMode.IMPLIED, 0x28, 1, 4); 
        }

        @Override
        public void exec() {
            setP(popAndIncPC());
        }
    }



    /*
     * Decrement and increment instructions
     */
    class DEX extends Instruction {
        DEX() {
            super("dex", AddressingMode.IMPLIED, 0xCA, 1, 2);
        }

        public void exec() {
            X = (X - 1) & 0xFF;
            updateNZ(X);
            incPC();
        }
    }

    class INX extends Instruction {
        INX() {
            super("inx", AddressingMode.IMPLIED, 0xE8, 1, 2);
        }

        public void exec() {
            X = (X + 1) & 0xFF;
            updateNZ(X);
            incPC();
        }
    }

    class DEY extends Instruction {
        DEY() {
            super("dey", AddressingMode.IMPLIED, 0x88, 1, 2);
        }

        public void exec() {
            Y = (Y - 1) & 0xFF;
            updateNZ(Y);
            incPC();
        }
    }

    class INY extends Instruction {
        INY() {
            super("iny", AddressingMode.IMPLIED, 0xC8, 1, 2);
        }

        public void exec() {
            Y = (Y + 1) & 0xFF;
            updateNZ(Y);
            incPC();
        }
    }

    abstract class DEC extends Instruction {
        DEC(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            int addr = getOperandAddress(this.addrMode);
            int val = (bus.read(addr) - 1) & 0xFF;
            updateNZ(val);
            bus.write(addr, val);
            incPC();
        }
    }

    class DECZeropage extends DEC {
        DECZeropage() {
            super("dec", AddressingMode.ZEROPAGE, 0xC6, 2, 5);
        }
    }

    class DECZeropageX extends DEC {
        DECZeropageX() {
            super("dec", AddressingMode.ZEROPAGE_X, 0xD6, 2, 6);
        }
    }

    class DECAbsolute extends DEC {
        DECAbsolute() {
            super("dec", AddressingMode.ABSOLUTE, 0xCE, 3, 6);
        }
    }

    class DECAbsoluteX extends DEC {
        DECAbsoluteX() {
            super("dec", AddressingMode.ABSOLUTE_X, 0xDE, 3, 7);
        }
    }

    abstract class INC extends Instruction {
        INC(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            int addr = getOperandAddress(this.addrMode);
            int val = (bus.read(addr) + 1) & 0xFF;
            updateNZ(val);
            bus.write(addr, val);
            incPC();
        }
    }

    class INCZeropage extends INC {
        INCZeropage() {
            super("inc", AddressingMode.ZEROPAGE, 0xE6, 2, 5);
        }
    }

    class INCZeropageX extends INC {
        INCZeropageX() {
            super("inc", AddressingMode.ZEROPAGE_X, 0xF6, 2, 6);
        }
    }

    class INCAbsolute extends INC {
        INCAbsolute() {
            super("inc", AddressingMode.ABSOLUTE, 0xEE, 3, 6);
        }
    }

    class INCAbsoluteX extends INC {
        INCAbsoluteX() {
            super("inc", AddressingMode.ABSOLUTE_X, 0xFE, 3, 7);
        }
    }

    class INCImplied extends Instruction {
        INCImplied() {
            super("inc", AddressingMode.IMPLIED, 0x1A, 1, 2);
        }

        public void exec() {
            A = (A + 1) & 0xFF;
            updateNZ(A);
            incPC();
        }
    }



    /*
     * Arithmetic instructions
     */


    abstract class ADC extends Instruction {
        ADC(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            assert !D : "Decimal mode not supported!";

            int val = getLoadVal(this.addrMode);
            boolean valSign = val > 127;
            boolean ASign = A > 127;
            
            A = A + val + (C ? 1 : 0);
            C = A > 255;
            A = A & 0xFF;
            updateNZ(A);
            V = (valSign == ASign) && (valSign != (A > 127));
            incPC();
        }
    }

    class ADCImmediate extends ADC {
        ADCImmediate() {
            super("adc", AddressingMode.IMMEDIATE, 0x69, 2, 2);
        }
    }

    class ADCZeropage extends ADC {
        ADCZeropage() {
            super("adc", AddressingMode.ZEROPAGE, 0x65, 2, 3);
        }
    }

    class ADCZeropageX extends ADC {
        ADCZeropageX() {
            super("adc", AddressingMode.ZEROPAGE_X, 0x75, 2, 4);
        }
    }

    class ADCAbsolute extends ADC {
        ADCAbsolute() {
            super("adc", AddressingMode.ABSOLUTE, 0x6D, 3, 4);
        }
    }

    class ADCAbsoluteX extends ADC {
        ADCAbsoluteX() {
            super("adc", AddressingMode.ABSOLUTE_X, 0x7D, 3, 4);
        }
    }

    class ADCAbsoluteY extends ADC {
        ADCAbsoluteY() {
            super("adc", AddressingMode.ABSOLUTE_Y, 0x79, 3, 4);
        }
    }

    class ADCXIndirect extends ADC {
        ADCXIndirect() {
            super("adc", AddressingMode.X_INDIRECT, 0x61, 2, 6);
        }
    }

    class ADCIndirectY extends ADC {
        ADCIndirectY() {
            super("adc", AddressingMode.INDIRECT_Y, 0x71, 2, 5);
        }
    }


    abstract class SBC extends Instruction {
        SBC(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            assert !D : "Decimal mode not supported!";

            // Take 1s complement of val and then perform an ADC
            int val = getLoadVal(this.addrMode) ^ 0xFF;
            boolean valSign = val > 127;
            boolean ASign = A > 127;
            
            A = A + val + (C ? 1 : 0);
            C = A > 255;
            A = A & 0xFF;
            updateNZ(A);
            V = (valSign == ASign) && (valSign != (A > 127));
            incPC();
        }
    }

    class SBCImmediate extends SBC {
        SBCImmediate() {
            super("sbc", AddressingMode.IMMEDIATE, 0xE9, 2, 2);
        }
    }

    class SBCZeropage extends SBC {
        SBCZeropage() {
            super("sbc", AddressingMode.ZEROPAGE, 0xE5, 2, 3);
        }
    }

    class SBCZeropageX extends SBC {
        SBCZeropageX() {
            super("sbc", AddressingMode.ZEROPAGE_X, 0xF5, 2, 4);
        }
    }

    class SBCAbsolute extends SBC {
        SBCAbsolute() {
            super("sbc", AddressingMode.ABSOLUTE, 0xED, 3, 4);
        }
    }

    class SBCAbsoluteX extends SBC {
        SBCAbsoluteX() {
            super("sbc", AddressingMode.ABSOLUTE_X, 0xFD, 3, 4);
        }
    }

    class SBCAbsoluteY extends SBC {
        SBCAbsoluteY() {
            super("sbc", AddressingMode.ABSOLUTE_Y, 0xF9, 3, 4);
        }
    }

    class SBCXIndirect extends SBC {
        SBCXIndirect() {
            super("sbc", AddressingMode.X_INDIRECT, 0xE1, 2, 6);
        }
    }

    class SBCIndirectY extends SBC {
        SBCIndirectY() {
            super("sbc", AddressingMode.INDIRECT_Y, 0xF1, 2, 5);
        }
    }
    




    /*
     * Logic instructions
     */
    abstract class AND extends Instruction {
        AND(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        @Override
        public void exec() {
            int val = getLoadVal(this.addrMode);
            A = A & val;
            updateNZ(A);
            incPC();
        }
    }

    class ANDImmediate extends AND {
        ANDImmediate() {
            super("and", AddressingMode.IMMEDIATE, 0x29, 2, 2);
        }
    }

    class ANDZeropage extends AND {
        ANDZeropage() {
            super("and", AddressingMode.ZEROPAGE, 0x25, 2, 3);
        }
    }

    class ANDZeropageX extends AND {
        ANDZeropageX() {
            super("and", AddressingMode.ZEROPAGE_X, 0x35, 2, 4);
        }
    }

    class ANDAbsolute extends AND {
        ANDAbsolute() {
            super("and", AddressingMode.ABSOLUTE, 0x2D, 3, 4);
        }
    }

    class ANDAbsoluteX extends AND {
        ANDAbsoluteX() {
            super("and", AddressingMode.ABSOLUTE_X, 0x3D, 3, 4);
        }
    }

    class ANDAbsoluteY extends AND {
        ANDAbsoluteY() {
            super("and", AddressingMode.ABSOLUTE_Y, 0x39, 3, 4);
        }
    }


    class ANDXIndirect extends AND {
        ANDXIndirect() {
            super("and", AddressingMode.X_INDIRECT, 0x21, 2, 6);
        }
    }

    class ANDIndirectY extends AND {
        ANDIndirectY() {
            super("and", AddressingMode.INDIRECT_Y, 0x31, 2, 5);
        }
    }


    abstract class EOR extends Instruction {
        EOR(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        @Override
        public void exec() {
            int val = getLoadVal(this.addrMode);
            A = A ^ val;
            updateNZ(A);
            incPC();
        }
    }

    class EORImmediate extends EOR {
        EORImmediate() {
            super("eor", AddressingMode.IMMEDIATE, 0x49, 2, 2);
        }
    }

    class EORZeropage extends EOR {
        EORZeropage() {
            super("eor", AddressingMode.ZEROPAGE, 0x45, 2, 3);
        }
    }

    class EORZeropageX extends EOR {
        EORZeropageX() {
            super("eor", AddressingMode.ZEROPAGE_X, 0x55, 2, 4);
        }
    }

    class EORAbsolute extends EOR {
        EORAbsolute() {
            super("eor", AddressingMode.ABSOLUTE, 0x4D, 3, 4);
        }
    }

    class EORAbsoluteX extends EOR {
        EORAbsoluteX() {
            super("eor", AddressingMode.ABSOLUTE_X, 0x5D, 3, 4);
        }
    }

    class EORAbsoluteY extends EOR {
        EORAbsoluteY() {
            super("eor", AddressingMode.ABSOLUTE_Y, 0x59, 3, 4);
        }
    }


    class EORXIndirect extends EOR {
        EORXIndirect() {
            super("eor", AddressingMode.X_INDIRECT, 0x41, 2, 6);
        }
    }

    class EORIndirectY extends EOR {
        EORIndirectY() {
            super("eor", AddressingMode.INDIRECT_Y, 0x51, 2, 5);
        }
    }

    abstract class ORA extends Instruction {
        ORA(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        @Override
        public void exec() {
            int val = getLoadVal(this.addrMode);
            A = A | val;
            updateNZ(A);
            incPC();
        }
    }

    class ORAImmediate extends ORA {
        ORAImmediate() {
            super("ora", AddressingMode.IMMEDIATE, 0x09, 2, 2);
        }
    }

    class ORAZeropage extends ORA {
        ORAZeropage() {
            super("ora", AddressingMode.ZEROPAGE, 0x05, 2, 3);
        }
    }

    class ORAZeropageX extends ORA {
        ORAZeropageX() {
            super("ora", AddressingMode.ZEROPAGE_X, 0x15, 2, 4);
        }
    }

    class ORAAbsolute extends ORA {
        ORAAbsolute() {
            super("ora", AddressingMode.ABSOLUTE, 0x0D, 3, 4);
        }
    }

    class ORAAbsoluteX extends ORA {
        ORAAbsoluteX() {
            super("ora", AddressingMode.ABSOLUTE_X, 0x1D, 3, 4);
        }
    }

    class ORAAbsoluteY extends ORA {
        ORAAbsoluteY() {
            super("ora", AddressingMode.ABSOLUTE_Y, 0x19, 3, 4);
        }
    }


    class ORAXIndirect extends ORA {
        ORAXIndirect() {
            super("ora", AddressingMode.X_INDIRECT, 0x01, 2, 6);
        }
    }

    class ORAIndirectY extends ORA {
        ORAIndirectY() {
            super("ora", AddressingMode.INDIRECT_Y, 0x11, 2, 5);
        }
    }





    /*
     * Shift and rotate instructions
     */

    abstract class ASL extends Instruction {
        ASL(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            int addr = getOperandAddress(this.addrMode);
            int val = bus.read(addr) << 1;
            C = val > 255;
            val = val & 0xFF;
            updateNZ(val);
            bus.write(addr, val);
            incPC();
        }
    }

    class ASLAccumulator extends Instruction {
        ASLAccumulator() {
            super("asl", AddressingMode.ACCUMULATOR, 0x0A, 1, 2);
        }

        public void exec() {
            A = A << 1;
            C = A > 255;
            A = A & 0xFF;
            updateNZ(A);
            incPC();
        }
    }

    class ASLZeropage extends ASL {
        ASLZeropage() {
            super("asl", AddressingMode.ZEROPAGE, 0x06, 2, 5);
        }
    }

    class ASLZeropageX extends ASL {
        ASLZeropageX() {
            super("asl", AddressingMode.ZEROPAGE_X, 0x16, 2, 6);
        }
    }

    class ASLAbsolute extends ASL {
        ASLAbsolute() {
            super("asl", AddressingMode.ABSOLUTE, 0x0E, 3, 6);
        }
    }

    class ASLAbsoluteX extends ASL {
        ASLAbsoluteX() {
            super("asl", AddressingMode.ABSOLUTE_X, 0x1E, 3, 7);
        }
    }



    abstract class LSR extends Instruction {
        LSR(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            int addr = getOperandAddress(this.addrMode);
            int val = bus.read(addr);
            C = (val & 1) == 1;
            val = val >> 1;
            updateNZ(val);
            bus.write(addr, val);
            incPC();
        }
    }

    class LSRAccumulator extends Instruction {
        LSRAccumulator() {
            super("lsr", AddressingMode.ACCUMULATOR, 0x4A, 1, 2);
        }

        public void exec() {
            C = (A & 1) == 1;
            A = A >> 1;
            updateNZ(A);
            incPC();
        }
    }

    class LSRZeropage extends LSR {
        LSRZeropage() {
            super("lsr", AddressingMode.ZEROPAGE, 0x46, 2, 5);
        }
    }

    class LSRZeropageX extends LSR {
        LSRZeropageX() {
            super("lsr", AddressingMode.ZEROPAGE_X, 0x56, 2, 6);
        }
    }

    class LSRAbsolute extends LSR {
        LSRAbsolute() {
            super("lsr", AddressingMode.ABSOLUTE, 0x4E, 3, 6);
        }
    }

    class LSRAbsoluteX extends LSR {
        LSRAbsoluteX() {
            super("lsr", AddressingMode.ABSOLUTE_X, 0x5E, 3, 7);
        }
    }


    abstract class ROL extends Instruction {
        ROL(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            int addr = getOperandAddress(this.addrMode);
            int val = bus.read(addr);
            val = (val << 1) | (C ? 1 : 0);
            C = val > 255;
            val = val & 0xFF;
            updateNZ(val);
            bus.write(addr, val);
            incPC();
        }
    }

    class ROLAccumulator extends Instruction {
        ROLAccumulator() {
            super("rol", AddressingMode.ACCUMULATOR, 0x2A, 1, 2);
        }

        public void exec() {
            A = (A << 1) | (C ? 1 : 0);
            C = A > 255;
            A = A & 0xFF;
            updateNZ(A);
            incPC();
        }
    }

    class ROLZeropage extends ROL {
        ROLZeropage() {
            super("rol", AddressingMode.ZEROPAGE, 0x26, 2, 5);
        }
    }

    class ROLZeropageX extends ROL {
        ROLZeropageX() {
            super("rol", AddressingMode.ZEROPAGE_X, 0x36, 2, 6);
        }
    }

    class ROLAbsolute extends ROL {
        ROLAbsolute() {
            super("rol", AddressingMode.ABSOLUTE, 0x2E, 3, 6);
        }
    }

    class ROLAbsoluteX extends ROL {
        ROLAbsoluteX() {
            super("rol", AddressingMode.ABSOLUTE_X, 0x3E, 3, 7);
        }
    }


    abstract class ROR extends Instruction {
        ROR(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            int addr = getOperandAddress(this.addrMode);
            int val = bus.read(addr);
            boolean oldC = C;
            C = (val & 1) == 1;
            val = (val >> 1) | (oldC ? 0x80 : 0);
            updateNZ(val);
            bus.write(addr, val);
            incPC();
        }
    }

    class RORAccumulator extends Instruction {
        RORAccumulator() {
            super("ror", AddressingMode.ACCUMULATOR, 0x6A, 1, 2);
        }

        public void exec() {
            boolean oldC = C;
            C = (A & 1) == 1;
            A = (A >> 1) | (oldC ? 0x80 : 0);
            updateNZ(A);
            incPC();
        }
    }

    class RORZeropage extends ROR {
        RORZeropage() {
            super("ror", AddressingMode.ZEROPAGE, 0x66, 2, 5);
        }
    }

    class RORZeropageX extends ROR {
        RORZeropageX() {
            super("ror", AddressingMode.ZEROPAGE_X, 0x76, 2, 6);
        }
    }

    class RORAbsolute extends ROR {
        RORAbsolute() {
            super("ror", AddressingMode.ABSOLUTE, 0x6E, 3, 6);
        }
    }

    class RORAbsoluteX extends ROR {
        RORAbsoluteX() {
            super("ror", AddressingMode.ABSOLUTE_X, 0x7E, 3, 7);
        }
    }


    /*
     * Flag instructions
     */

    class CLC extends Instruction {
        CLC() {
            super("clc", AddressingMode.IMPLIED, 0x18, 1, 2);
        }

        public void exec() {
            C = false;
            incPC();            
        }
    }

    class CLD extends Instruction {
        CLD() {
            super("cld", AddressingMode.IMPLIED, 0xD8, 1, 2);
        }

        public void exec() {
            D = false;
            incPC();            
        }
    }

    class CLI extends Instruction {
        CLI() {
            super("cli", AddressingMode.IMPLIED, 0x58, 1, 2);
        }

        public void exec() {
            I = false;
            incPC();            
        }
    }

    class CLV extends Instruction {
        CLV() {
            super("clv", AddressingMode.IMPLIED, 0xB8, 1, 2);
        }

        public void exec() {
            V = false;
            incPC();            
        }
    }

    class SEC extends Instruction {
        SEC() {
            super("sec", AddressingMode.IMPLIED, 0x38, 1, 2);
        }

        public void exec() {
            C = true;
            incPC();            
        }
    }

    class SED extends Instruction {
        SED() {
            super("sed", AddressingMode.IMPLIED, 0xF8, 1, 2);
        }

        public void exec() {
            D = true;
            incPC();            
        }
    }

    class SEI extends Instruction {
        SEI() {
            super("sei", AddressingMode.IMPLIED, 0x78, 1, 2);
        }

        public void exec() {
            I = true;
            incPC();            
        }
    }


    /*
     * Comparison instructions
     */

    abstract class CMP extends Instruction {
        CMP(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }
        
        public void exec() {
            int val = getLoadVal(this.addrMode);
            Z = A == val;
            C = A >= val;
            N = ((A - val) & 0x80) > 0;
            incPC();
        }
    }

    class CMPImmediate extends CMP {
        CMPImmediate() {
            super("cmp", AddressingMode.IMMEDIATE, 0xC9, 2, 2);
        }
    }

    class CMPZeropage extends CMP {
        CMPZeropage() {
            super("cmp", AddressingMode.ZEROPAGE, 0xC5, 2, 3);
        }
    }

    class CMPZeropageX extends CMP {
        CMPZeropageX() {
            super("cmp", AddressingMode.ZEROPAGE_X, 0xD5, 2, 4);
        }
    }

    class CMPAbsolute extends CMP {
        CMPAbsolute() {
            super("cmp", AddressingMode.ABSOLUTE, 0xCD, 3, 4);
        }
    }


    class CMPAbsoluteX extends CMP {
        CMPAbsoluteX() {
            super("cmp", AddressingMode.ABSOLUTE_X, 0xDD, 3, 4);
        }
    }

    class CMPAbsoluteY extends CMP {
        CMPAbsoluteY() {
            super("cmp", AddressingMode.ABSOLUTE_Y, 0xD9, 3, 4);
        }
    }

    class CMPXIndirect extends CMP {
        CMPXIndirect() {
            super("cmp", AddressingMode.X_INDIRECT, 0xC1, 2, 6);
        }
    }

    class CMPIndirectY extends CMP {
        CMPIndirectY() {
            super("cmp", AddressingMode.INDIRECT_Y, 0xD1, 2, 5);
        }
    }


    abstract class CPX extends Instruction {
        CPX(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }
        
        public void exec() {
            int val = getLoadVal(this.addrMode);
            Z = X == val;
            C = X >= val;
            N = ((X - val) & 0x80) > 0;
            incPC();
        }
    }

    class CPXImmediate extends CPX {
        CPXImmediate() {
            super("cpx", AddressingMode.IMMEDIATE, 0xE0, 2, 2);
        }
    }

    class CPXZeropage extends CPX {
        CPXZeropage() {
            super("cpx", AddressingMode.ZEROPAGE, 0xE4, 2, 3);
        }
    }


    class CPXAbsolute extends CPX {
        CPXAbsolute() {
            super("cpx", AddressingMode.ABSOLUTE, 0xEC, 3, 4);
        }
    }

    abstract class CPY extends Instruction {
        CPY(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }
        
        public void exec() {
            int val = getLoadVal(this.addrMode);
            Z = Y == val;
            C = Y >= val;
            N = ((Y - val) & 0x80) > 0;
            incPC();
        }
    }


    class CPYImmediate extends CPY {
        CPYImmediate() {
            super("cpy", AddressingMode.IMMEDIATE, 0xC0, 2, 2);
        }
    }

    class CPYZeropage extends CPY {
        CPYZeropage() {
            super("cpy", AddressingMode.ZEROPAGE, 0xC4, 2, 3);
        }
    }


    class CPYAbsolute extends CPY {
        CPYAbsolute() {
            super("cpy", AddressingMode.ABSOLUTE, 0xCC, 3, 4);
        }
    }







    /*
     * Branch instructions
     */

    private void branch(boolean condition) {
        incPC();
        byte offset = (byte)bus.read(PC);
        if (condition) PC = (PC + offset) & 0xFFFF;
        incPC();
    }

    class BCC extends Instruction {
        BCC() {
            super("bcc", AddressingMode.RELATIVE, 0x90, 2, 2);
        }

        public void exec() {
            branch(!C);
        }
    }

    class BCS extends Instruction {
        BCS() {
            super("bcs", AddressingMode.RELATIVE, 0xB0, 2, 2);
        }

        public void exec() {
            branch(C);
        }
    }

    class BEQ extends Instruction {
        BEQ() {
            super("beq", AddressingMode.RELATIVE, 0xF0, 2, 2);
        }

        public void exec() {
            branch(Z);
        }
    }

    class BNE extends Instruction {
        BNE() {
            super("bne", AddressingMode.RELATIVE, 0xD0, 2, 2);
        }

        public void exec() {
            branch(!Z);
        }
    }

    class BPL extends Instruction {
        BPL() {
            super("bpl", AddressingMode.RELATIVE, 0x10, 2, 2);
        }

        public void exec() {
            branch(!N);
        }
    }

    class BMI extends Instruction {
        BMI() {
            super("bmi", AddressingMode.RELATIVE, 0x30, 2, 2);
        }

        public void exec() {
            branch(N);
        }
    }

    class BVC extends Instruction {
        BVC() {
            super("bvc", AddressingMode.RELATIVE, 0x50, 2, 2);
        }

        public void exec() {
            branch(!V);
        }
    }

    class BVS extends Instruction {
        BVS() {
            super("bvs", AddressingMode.RELATIVE, 0x70, 2, 2);
        }

        public void exec() {
            branch(V);
        }
    }

    class BRA extends Instruction {
        BRA() {
            super("bra", AddressingMode.RELATIVE, 0x80, 2, 3);
        }

        public void exec() {
            branch(true);
        }
    }

    



    /*
     * Jump and subroutine instructions
     */

    abstract class JMP extends Instruction {
        JMP(String mnemonic, CPU.AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super(mnemonic, addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            PC = getOperandAddress(this.addrMode);
        }
    }

    class JMPAbsolute extends JMP {
        JMPAbsolute() {
            super("jmp", AddressingMode.ABSOLUTE, 0x4C, 3, 3);
        }
    }

    class JMPIndirect extends JMP {
        JMPIndirect() {
            super("jmp", AddressingMode.INDIRECT, 0x6C, 3, 5);
        }
    }


    class JSR extends Instruction {
        JSR() {
            super("jsr", AddressingMode.ABSOLUTE, 0x20, 3, 6);
        }

        @Override
        public void exec() {
            incPC();
            int addrLo = bus.read(PC);
            incPC();
            push(PC >> 8);
            push(PC & 0xff);
            int addrHi = bus.read(PC);
            PC = addrHi * 0x100 + addrLo;
        }
    }

    class RTS extends Instruction {
        RTS() {
            super("rts", AddressingMode.IMPLIED, 0x60, 1, 6);
        }

        @Override
        public void exec() {
            S = (S + 1) & 0xFF;
            int addrLo = bus.read(0x0100 + S);
            S = (S + 1) & 0xFF;
            int addrHi = bus.read(0x0100 + S);
            PC = addrHi * 0x100 + addrLo;
            incPC();
        }
    }



    /*
     * Interrupt instructions
     */


    class RTI extends Instruction {
        RTI() {
            super("rti", AddressingMode.IMPLIED, 0x40, 1, 6);
        }

        public void exec() {
            setP(pop());
            int pcLo = pop();
            int pcHi = pop();

            PC = (pcHi * 0x100) + pcLo;
        }
    }


    class BRK extends Instruction {
        BRK() {
            super("brk", AddressingMode.IMPLIED, 0x00, 1, 7);
        }

        public void exec() {
            incPC();
            incPC();
            doIRQ(true);
        }
    }


    /*
     * Other instructions
     */
    class NOP extends Instruction {
        NOP() {
            super("nop", AddressingMode.IMPLIED, 0xEA, 1, 2);
        }

        public void exec() {
            incPC();
        }
    }

    abstract class BIT extends Instruction {
        BIT(AddressingMode addrMode, int opcode, int bytes, int cycles) {
            super("bit", addrMode, opcode, bytes, cycles);
        }

        public void exec() {
            int val = getLoadVal(addrMode);
            N = (val & 0b10000000) > 0;
            V = (val & 0b01000000) > 0;
            Z = (val & A) == 0;
            incPC();
        }
    }

    class BITZeropage extends BIT {
        BITZeropage() {
            super(AddressingMode.ZEROPAGE, 0x24, 2, 3);
        }
    }

    class BITAbsolute extends BIT {
        BITAbsolute() {
            super(AddressingMode.ABSOLUTE, 0x2C, 3, 4);
        }
    }

    class BITImmediate extends Instruction {
        BITImmediate() {
            super("bit", AddressingMode.IMMEDIATE, 0x89, 2, 2);
        }

        public void exec() {
            int val = getLoadVal(addrMode);
            Z = (val & A) == 0;
            incPC();
        }
    }


    /**
     * 65C02 bit instructions
     */
    class BBR extends Instruction {
        private int mask;

        BBR(int bit, int opcode) {
            super("bbr" + bit, AddressingMode.ZEROPAGE, opcode, 3, 5);
            mask = 0b00000001 << bit;
        }

        public void exec() {
            incPC();
            int zpAddr = bus.read(PC);
            branch((mask & bus.read(zpAddr)) == 0);
        }
    }

    class BBR0 extends BBR {
        BBR0() {
            super(0, 0x0F);
        }
    }

    class BBR1 extends BBR {
        BBR1() {
            super(1, 0x1F);
        }
    }

    class BBR2 extends BBR {
        BBR2() {
            super(2, 0x2F);
        }
    }

    class BBR3 extends BBR {
        BBR3() {
            super(3, 0x3F);
        }
    }

    class BBR4 extends BBR {
        BBR4() {
            super(4, 0x4F);
        }
    }

    class BBR5 extends BBR {
        BBR5() {
            super(5, 0x5F);
        }
    }

    class BBR6 extends BBR {
        BBR6() {
            super(6, 0x6F);
        }
    }

    class BBR7 extends BBR {
        BBR7() {
            super(7, 0x7F);
        }
    }

    class BBS extends Instruction {
        private int mask;

        BBS(int bit, int opcode) {
            super("bbs" + bit, AddressingMode.ZEROPAGE, opcode, 3, 5);
            mask = 0b00000001 << bit;
        }

        public void exec() {
            incPC();
            int zpAddr = bus.read(PC);
            branch((mask & bus.read(zpAddr)) != 0);
        }
    }

    class BBS0 extends BBS {
        BBS0() {
            super(0, 0x8F);
        }
    }

    class BBS1 extends BBS {
        BBS1() {
            super(1, 0x9F);
        }
    }

    class BBS2 extends BBS {
        BBS2() {
            super(2, 0xAF);
        }
    }

    class BBS3 extends BBS {
        BBS3() {
            super(3, 0xBF);
        }
    }

    class BBS4 extends BBS {
        BBS4() {
            super(4, 0xCF);
        }
    }

    class BBS5 extends BBS {
        BBS5() {
            super(5, 0xDF);
        }
    }

    class BBS6 extends BBS {
        BBS6() {
            super(6, 0xEF);
        }
    }

    class BBS7 extends BBS {
        BBS7() {
            super(7, 0xFF);
        }
    }

    class RMB extends Instruction {
        private int mask;

        RMB(int bit, int opcode) {
            super("rmb" + bit, AddressingMode.ZEROPAGE, opcode, 2, 5);
            mask = ~(0b00000001 << bit);
        }

        public void exec() {
            incPC();
            int zpAddr = bus.read(PC);
            int val = bus.read(zpAddr) & mask;
            bus.write(zpAddr, val);
            incPC();
        }
    }

    class RMB0 extends RMB {
        RMB0() {
            super(0, 0x07);
        }
    }

    class RMB1 extends RMB {
        RMB1() {
            super(1, 0x17);
        }
    }

    class RMB2 extends RMB {
        RMB2() {
            super(2, 0x27);
        }
    }

    class RMB3 extends RMB {
        RMB3() {
            super(3, 0x37);
        }
    }

    class RMB4 extends RMB {
        RMB4() {
            super(4, 0x47);
        }
    }

    class RMB5 extends RMB {
        RMB5() {
            super(5, 0x57);
        }
    }

    class RMB6 extends RMB {
        RMB6() {
            super(6, 0x67);
        }
    }

    class RMB7 extends RMB {
        RMB7() {
            super(7, 0x77);
        }
    }

    class SMB extends Instruction {
        private int mask;

        SMB(int bit, int opcode) {
            super("smb" + bit, AddressingMode.ZEROPAGE, opcode, 2, 5);
            mask = 0b00000001 << bit;
        }

        public void exec() {
            incPC();
            int zpAddr = bus.read(PC);
            int val = bus.read(zpAddr) | mask;
            bus.write(zpAddr, val);
            incPC();
        }
    }

    class SMB0 extends SMB {
        SMB0() {
            super(0, 0x87);
        }
    }

    class SMB1 extends SMB {
        SMB1() {
            super(1, 0x97);
        }
    }

    class SMB2 extends SMB {
        SMB2() {
            super(2, 0xA7);
        }
    }

    class SMB3 extends SMB {
        SMB3() {
            super(3, 0xB7);
        }
    }

    class SMB4 extends SMB {
        SMB4() {
            super(4, 0xC7);
        }
    }

    class SMB5 extends SMB {
        SMB5() {
            super(5, 0xD7);
        }
    }

    class SMB6 extends SMB {
        SMB6() {
            super(6, 0xE7);
        }
    }

    class SMB7 extends SMB {
        SMB7() {
            super(7, 0xF7);
        }
    }

    // registers
    private int PC;
    private int A;
    private int X;
    private int Y;
    private int S;

    // flags
    private boolean C;
    private boolean N;
    private boolean Z;
    private boolean V;
    private boolean I;
    private boolean D;

    private Bus bus;

    private Instruction[] instructions;

    private long cycleCount;


    public CPU(Bus bus) {
        this.PC = 0xFFFC;
        this.A = 0;
        this.X = 0;
        this.Y = 0;

        this.cycleCount = 0;

        this.bus = bus;

        Instruction[] allInstructions = {
            new LDAImmediate(), new LDAZeropage(), new LDAZeropageX(), new LDAAbsolute(), new LDAAbsoluteX(), new LDAAbsoluteY(), new LDAXIndirect(), new LDAIndirectY(),
            new LDXImmediate(), new LDXAbsolute(), new LDXAbsoluteY(), new LDXZeropage(), new LDXZeropageY(),
            new LDYImmediate(), new LDYAbsolute(), new LDYAbsoluteX(), new LDYZeropage(), new LDYZeropageX(),
            new STAZeropage(), new STAZeropageX(), new STAAbsolute(), new STAAbsoluteX(), new STAAbsoluteY(), new STAXIndirect(), new STAIndirectY(),
            new STXZeropage(), new STXZeropageY(), new STXAbsolute(),
            new STYZeropage(), new STYZeropageX(), new STYAbsolute(),
            new STZZeropage(), new STZZeropageX(), new STZAbsolute(), new STZAbsoluteX(),
            new TXA(), new TYA(), new TSX(), new TAX(), new TAY(), new TXS(),
            new PHA(), new PLA(), new PHP(), new PLP(), new PHX(), new PLX(), new PHY(), new PLY(),
            new INX(), new DEX(), new INY(), new DEY(), 
            new DECZeropage(), new DECZeropageX(), new DECAbsolute(), new DECAbsoluteX(),
            new INCZeropage(), new INCZeropageX(), new INCAbsolute(), new INCAbsoluteX(), new INCImplied(),
            new ADCImmediate(), new ADCZeropage(), new ADCZeropageX(), new ADCAbsolute(), new ADCAbsoluteX(), new ADCAbsoluteY(), new ADCXIndirect(), new ADCIndirectY(),
            new SBCImmediate(), new SBCZeropage(), new SBCZeropageX(), new SBCAbsolute(), new SBCAbsoluteX(), new SBCAbsoluteY(), new SBCXIndirect(), new SBCIndirectY(),
            new ANDImmediate(), new ANDZeropage(), new ANDZeropageX(), new ANDAbsolute(), new ANDAbsoluteX(), new ANDAbsoluteY(), new ANDXIndirect(), new ANDIndirectY(),
            new EORImmediate(), new EORZeropage(), new EORZeropageX(), new EORAbsolute(), new EORAbsoluteX(), new EORAbsoluteY(), new EORXIndirect(), new EORIndirectY(),
            new ORAImmediate(), new ORAZeropage(), new ORAZeropageX(), new ORAAbsolute(), new ORAAbsoluteX(), new ORAAbsoluteY(), new ORAXIndirect(), new ORAIndirectY(),
            new ASLAccumulator(), new ASLZeropage(), new ASLZeropageX(), new ASLAbsolute(), new ASLAbsoluteX(),
            new LSRAccumulator(), new LSRZeropage(), new LSRZeropageX(), new LSRAbsolute(), new LSRAbsoluteX(),
            new ROLAccumulator(), new ROLZeropage(), new ROLZeropageX(), new ROLAbsolute(), new ROLAbsoluteX(),
            new RORAccumulator(), new RORZeropage(), new RORZeropageX(), new RORAbsolute(), new RORAbsoluteX(),
            new CLC(), new CLD(), new CLI(), new CLV(),
            new SEC(), new SED(), new SEI(),
            new CMPImmediate(), new CMPZeropage(), new CMPZeropageX(), new CMPAbsolute(), new CMPAbsoluteX(), new CMPAbsoluteY(), new CMPXIndirect(), new CMPIndirectY(),
            new CPXImmediate(), new CPXZeropage(), new CPXAbsolute(),
            new CPYImmediate(), new CPYZeropage(), new CPYAbsolute(),
            new BEQ(), new BNE(), new BCC(), new BCS(), new BMI(), new BPL(), new BVC(), new BVS(), new BRA(),
            new JMPAbsolute(), new JMPIndirect(),
            new JSR(), new RTS(),
            new RTI(),
            new NOP(),
            new BRK(),
            new BITAbsolute(), new BITZeropage(), new BITImmediate(),
            new BBR0(), new BBR1(), new BBR2(), new BBR3(), new BBR4(), new BBR5(), new BBR6(), new BBR7(),
            new BBS0(), new BBS1(), new BBS2(), new BBS3(), new BBS4(), new BBS5(), new BBS6(), new BBS7(),
            new RMB0(), new RMB1(), new RMB2(), new RMB3(), new RMB4(), new RMB5(), new RMB6(), new RMB7(),
            new SMB0(), new SMB1(), new SMB2(), new SMB3(), new SMB4(), new SMB5(), new SMB6(), new SMB7(),
        };

        this.instructions = new Instruction[0x100];
        for (Instruction inst : allInstructions) {
            this.instructions[inst.opcode] = inst;
        }

    }


    public void printStatus() {
        System.out.println("A: " + A + "   X: " + X + "   Y: " + Y + "   PC: " + PC + "   NV-BDIZC: " +  String.format("%8s", Integer.toBinaryString(getP())).replace(' ', '0'));
    }

    public long getCycleCount() {
        return cycleCount;
    }


    public void reset() {
        PC = bus.read(0xFFFC) + bus.read(0xFFFD) * 0x100;
        I = false;
        D = false;
        S = 0xFF;
    }

    public void step() {
        if (bus.getIRQ() && !I) {
            doIRQ(false);
        } else {
            int opcode = bus.read(PC);
            Instruction inst = this.instructions[opcode];
            
            assert inst != null : "Unsupported opcode " + String.format("0x%02x", opcode);
    
            // System.out.println(inst.mnemonic);
            cycleCount += inst.cycles;
            inst.exec();
        }
    }



    private void incPC() {
        PC = (PC + 1) & 0xFFFF;
    }

    private void updateNZ(int val) {
        Z = val == 0;
        N = val > 127;
    } 

    private int getOperandAddress(AddressingMode addrMode) {
        int operand = -1;
        int addrLo;
        int addrHi;
        if (addrMode.operandBytes() == 1) {
            incPC();
            operand = this.bus.read(PC);
        } else if (addrMode.operandBytes() == 2) {
            this.incPC();
            addrLo = bus.read(PC);
            incPC();
            addrHi = bus.read(PC);
            operand =  addrLo + addrHi * 0x100;
        }

        switch (addrMode) {
            case ZEROPAGE:
                return operand;
            case ZEROPAGE_X:
                return (operand + X) & 0xFF;
            case ZEROPAGE_Y:
                return (operand + Y) & 0xFF;                
            case ABSOLUTE:
                return operand;
            case ABSOLUTE_X:
                return (operand + X) & 0xFFFF;
            case ABSOLUTE_Y:
                return (operand + Y) & 0xFFFF;
            case X_INDIRECT:
                int zpAddr = (operand + X) & 0xFF;
                addrLo = bus.read(zpAddr);
                addrHi = bus.read(zpAddr + 1);
                return addrLo + addrHi * 0x100;
            case INDIRECT_Y:
                addrLo = bus.read(operand);
                addrHi = bus.read(operand + 1);
                return (addrLo + addrHi * 0x100 + Y) & 0xFFFF;
            case INDIRECT:
                addrLo = bus.read(operand);
                addrHi = bus.read(operand + 1);
                return (addrLo + addrHi * 0x100) & 0xFFFF;
            default:
                assert false : "Invalid addressing mode " + addrMode;
                return -1;
        }
    }

    private int getLoadVal(AddressingMode addrMode) {
        if (addrMode == AddressingMode.IMMEDIATE) {
            incPC();
            return bus.read(PC);
        }

        return bus.read(getOperandAddress(addrMode));
    }

    private int getP() {
        int P = 0;
        boolean[] flags = {C, Z, I, D, false, true, V, N};
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) P |= 1 << i;
        }

        return P;
    }

    private void setP(int P) {
        C = (P & 0b00000001) != 0;
        Z = (P & 0b00000010) != 0;
        I = (P & 0b00000100) != 0;
        D = (P & 0b00001000) != 0;
        V = (P & 0b01000000) != 0;
        N = (P & 0b10000000) != 0;
    }

    private void doIRQ(boolean isBreak) {
        // System.out.println("IRQ!");
        push(PC >> 8);
        push(PC & 0xff);
        
        int P = getP();
        if (isBreak) {
            P = P | 0x10;
        }

        push(P);

        int vectorLo = bus.read(0xFFFE);
        int vectorHi = bus.read(0xFFFF);

        PC = vectorHi * 0x100 + vectorLo;
        I = true;
    }
}
