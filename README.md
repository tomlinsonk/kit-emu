# Emulator for the KiT 6502 Computer
This is an emulator for the KiT 6502-based computer I've built. The emulator has all the functionality of the real hardware except NES controller input (not currently planned).

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
- `SID.java`: emulates the SID sound chip (6581 by default), clocked at 1.8432 MHz like the KiT's, so notes play at the same pitch as on the hardware. Plays through your computer's audio output.
- `ssd.bin`: the binary contents of the SSD, loaded on start and saved on exit

## Instructions
To run the emulator, just do `javac *.java` and `java KiT`. See my [KiT Code](https://github.com/tomlinsonk/kit-6502-code) repo for programs that you can load over the emulated serial port. (Note: they need to be assembled with KickAssembler.)

(Looking back on this, I hardcoded some paths that you'll need to update.... TODO: make these configurable.)

### Headless mode
For compiler test automation, run `java -Djava.awt.headless=true KiT --headless` (the `-D` flag keeps macOS from opening it as an app and stealing focus). Headless mode loads a `.prg` over the emulated serial port, types `l`, Enter, `r`, Enter into the monitor, and runs until the program hits the emulator breakpoint instruction (`0xBB`). It then prints the CPU status and the screen text and exits with status 0.

Options:
- `--prg path/to/file.prg`: program to load (default: `/Users/tomlinsonk/projects/6502/kcc/out.prg`)
- `--timeout-ms 5000`: dump state and exit with status 2 if no breakpoint is reached in time
- `--keys "l\nr\ns\n"`: override the scripted monitor input
- `--dump-screen-hex`: also print the raw text VRAM bytes
- `--count-cycles 1000-4fff`: also print `Cycles: N`, the CPU cycles spent on instructions in that address range (hex, inclusive). Interrupt handlers and the monitor run in ROM, so they're excluded when the range covers only the program.

`--no-save-ssd` (headless or GUI) keeps `ssd.bin` unchanged on exit; SSD writes only last for that run.

### Sound

I made a sound card for the KiT a while back and assumed it would be hardware-only, since emulating a SID seemed beyond my ability and knowledge. Realistically, it would never have happened. So... I asked Claude Opus to do it. Behold, `SID.java`. And, while I was at it, I had Claude code up a C64 `.sid` to KiT program converter, along with a music player for the KiT. So now I can listen to Wizball (by [Martin Galway(https://en.wikipedia.org/wiki/Martin_Galway)) and Monty on the Run (by [Rob Hubbard](https://en.wikipedia.org/wiki/Martin_Galway)) on the KiT, and the KiT emulator. Incredible. 

To try it out, run `F10` to run the SID-play program, select a tune with up and down arrow, and play with enter.

Command line options (headless or GUI):
- `--sid-model 8580`: emulate an 8580 instead of a 6581
- `--record-sid out.wav`: also record everything the SID plays to a WAV file
- `--no-sound`: don't play sound

## Planned features
Crossed out = done!
- ~~Use unused opcode to signal an emulator breakpoint~~: 0xBB
- ~~When a breakpoint is reached, open a debugging console~~
    - ~~Debugging console should also viewing memory and registers~~
    - ~~Ability to resume execution~~
    - Show the last executed instruction mnemonic and argument
    - More advanced feature: allow memory and registers to be modified
