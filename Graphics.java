import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.imageio.ImageIO;

public class Graphics implements BusListener {
    private Bus bus;
    private VIA via;

    public static final int SCREEN_WIDTH = 256;
    public static final int SCREEN_HEIGHT = 192;

    private static final int DARK_GREEN = (new Color(24, 62, 12)).getRGB(); 
    private static final int GREEN = (new Color(117, 250, 76)).getRGB(); 
    private static final int RED = (new Color(235, 51, 35)).getRGB(); 
    private static final int BLUE = (new Color(0, 30, 245)).getRGB(); 
    private static final int YELLOW = (new Color(255, 254, 84)).getRGB(); 
    
    private static final int[][] CG1_PIXELS = {
        {GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN},
        {YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW},
        {BLUE, BLUE, BLUE, BLUE, BLUE, BLUE, BLUE, BLUE, BLUE, BLUE, BLUE, BLUE},
        {RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED}
    };
    
    private static final int[][] RG1_PIXELS = {
        {DARK_GREEN, DARK_GREEN, DARK_GREEN, DARK_GREEN, DARK_GREEN, DARK_GREEN, DARK_GREEN, DARK_GREEN, DARK_GREEN},
        {GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN}
    };   


    private static final int MODE_TEXT = 0b00000000;
    private static final int MODE_SG6 = 0b00100000;
    private static final int MODE_CG1 = 0b00000001;
    private static final int MODE_RG1 = 0b00000011;
    private static final int MODE_CG2 = 0b00000101;
    private static final int MODE_RG2 = 0b00000111;
    private static final int MODE_CG3 = 0b00001001;
    private static final int MODE_RG3 = 0b00001011;
    private static final int MODE_CG6 = 0b00001101;
    private static final int MODE_RG6 = 0b00001111;

    private int vramStartAddr;
    private AtomicIntegerArray vram;

    private BufferedImage img;

    private int[][][] charSet;

    public Graphics(Bus bus, VIA via, int vramStartAddr, int vramEndAddr) {
        this.bus = bus;
        this.via = via;
        this.vramStartAddr = vramStartAddr;

        this.vram = new AtomicIntegerArray(vramEndAddr - vramStartAddr + 1);
        this.img = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);

        charSet = loadCharSet();

        updateImage();
    }

    public void activate() {

        int vramAddr = bus.getAddr() - vramStartAddr;
        
        if (bus.readBitSet()) {
            bus.setData(vram.get(vramAddr));
        } else {
            vram.set(vramAddr, bus.getData());
        }
    }

    public int get(int i) {
        return vram.get(i);
    }

    public void updateImage() {
        // int i = 0;

        int videoMode = via.getPortA();

        if (videoMode == MODE_TEXT) {
            for (int row = 0; row < 16; row++) {
                for (int col = 0; col < 32; col++) {
                    int[][] character = charSet[vram.get(row * 32 + col)];
    
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 12; y++) {
                            img.setRGB(col * 8 + x, row * 12 + y, character[y][x]);
                        }
                    }
                }
            }
        } else if (videoMode == MODE_CG1) {
            int vramByte;
            for (int i = 0; i < 1024; i++) {
                vramByte = vram.get(i);

                img.setRGB((i % 16) * 16, (i / 16) * 3, 4, 3, CG1_PIXELS[vramByte >> 6], 0, 1);
                img.setRGB((i % 16) * 16 + 4, (i / 16) * 3, 4, 3, CG1_PIXELS[(vramByte >> 4) & 0b00000011], 0, 1);
                img.setRGB((i % 16) * 16 + 8, (i / 16) * 3, 4, 3, CG1_PIXELS[(vramByte >> 2) & 0b00000011], 0, 1);
                img.setRGB((i % 16) * 16 + 12, (i / 16) * 3, 4, 3, CG1_PIXELS[vramByte & 0b00000011], 0, 1);
            }
        } else if (videoMode == MODE_RG1) {
            int vramByte;
            for (int i = 0; i < 1024; i++) {
                vramByte = vram.get(i);

                img.setRGB((i % 16) * 16, (i / 16) * 3, 2, 3, RG1_PIXELS[(vramByte >> 7) & 1], 0, 1);
                img.setRGB((i % 16) * 16 + 2, (i / 16) * 3, 2, 3, RG1_PIXELS[(vramByte >> 6) & 1], 0, 1);
                img.setRGB((i % 16) * 16 + 4, (i / 16) * 3, 2, 3, RG1_PIXELS[(vramByte >> 5) & 1], 0, 1);
                img.setRGB((i % 16) * 16 + 6, (i / 16) * 3, 2, 3, RG1_PIXELS[(vramByte >> 4) & 1], 0, 1);
                img.setRGB((i % 16) * 16 + 8, (i / 16) * 3, 2, 3, RG1_PIXELS[(vramByte >> 3) & 1], 0, 1);
                img.setRGB((i % 16) * 16 + 10, (i / 16) * 3, 2, 3, RG1_PIXELS[(vramByte >> 2) & 1], 0, 1);
                img.setRGB((i % 16) * 16 + 12, (i / 16) * 3, 2, 3, RG1_PIXELS[(vramByte >> 1) & 1], 0, 1);
                img.setRGB((i % 16) * 16 + 14, (i / 16) * 3, 2, 3, RG1_PIXELS[vramByte & 1], 0, 1);

            }
        }
        
    }

    public BufferedImage getImage() {
        return img;
    }

    private int[][][] loadCharSet() {
        BufferedImage charSetImg = null;
        try {
            charSetImg = ImageIO.read(new File("character-set.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int[][][] charSet = new int[256][12][8];


        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 32; col++) {
                int charIdx = row * 32 + col;

                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 12; y++) {
                        charSet[charIdx][y][x] = charSetImg.getRGB(col * 8 + x, row * 12 + y);
                    }
                }
            }
        }

        return charSet;
        
    }
}
