# Emulator for the KiT 6502 Computer
This is a (work in progress) emulator for the KiT 6502-based computer I've built.

For more info on the KiT, see https://www.cs.cornell.edu/~kt/categories/6502/.

## Emulator architecture

The main file is `KiT.java`, which creates all of the componenets, runs the main loop, and initializes the GUI and keyboard input. Each major component of the KiT is modeled with its own class:
- `CPU.java`: the heart of the KiT, a 65C02 emulator (all 6502 instructions supported, some 65C02 instructions missing for now).
- `Bus.java`: the glue tying components together. Includes the address and data buses, the interrupt line, and the R/W signal. Whenever components need to talk to each other, it happens through the bus. 
- `AddressDecoder.java`: activates components that listen to the bus whenever their address range is selected
- `RAM.java`: emulates the RAM chip. Reads and writes happen through the bus.
- `ROM.java`: emulates the ROM chip. Initialized according to a 6502 binary file.
- `Graphics.java`: emulates the MC6847-based KiT graphics card (only text mode supported so far).
- `VIA.java`: emulates the 65C22 VIA chip. Can request interrupts.
- `SSD.java`: emulates a 256 Kb SSD using the SST39SF020A flash chip
- `ssd.bin`: the binary contents of the SSD, loaded on start and saved on exit
