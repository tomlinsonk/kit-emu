# Emulator for the KiT 6502 Computer
This is an emulator for the KiT 6502-based computer I've built. The emulator has all the functionality of the real hardware except sound and NES controller input (neither currently planned).

For more info on the KiT, see https://www.cs.cornell.edu/~kt/categories/6502/.

## Emulator architecture

The main file is `KiT.java`, which creates all of the componenets, runs the main loop, and initializes the GUI and keyboard input. Each major component of the KiT is modeled with its own class:
- `CPU.java`: the heart of the KiT, a 65C02 emulator (all 6502 instructions supported, some 65C02 instructions missing for now).
- `Bus.java`: the glue tying components together. Includes the address and data buses, the interrupt line, and the R/W signal. Whenever components need to talk to each other, it happens through the bus. 
- `AddressDecoder.java`: activates components that listen to the bus whenever their address range is selected
- `RAM.java`: emulates the RAM chip. Reads and writes happen through the bus.
- `ROM.java`: emulates the ROM chip. Initialized according to a 6502 binary file.
- `Graphics.java`: emulates the MC6847-based KiT graphics card.
- `VIA.java`: emulates the 65C22 VIA chip. Can request interrupts.
- `SSD.java`: emulates a 256 Kb SSD using the SST39SF020A flash chip
- `SID.java`: a dummy class that occupies the SID sound card's address space but does nothing (just exists so programs with sound work on the emulator, but no sound plays)
- `ssd.bin`: the binary contents of the SSD, loaded on start and saved on exit

## Instructions
To run the emulator, just do `javac *.java` and `java KiT`. See my [KiT Code](https://github.com/tomlinsonk/kit-6502-code) repo for programs that you can load over the emulated serial port. (Note: they need to be assembled with KickAssembler.)

## Planned features
Crossed out = done!
- ~~Use unused opcode to signal an emulator breakpoint~~: 0xBB
- ~~When a breakpoint is reached, open a debugging console~~
    - ~~Debugging console should also viewing memory and registers~~
    - ~~Ability to resume execution~~
    - More advanced feature: allow memory and registers to be modified
