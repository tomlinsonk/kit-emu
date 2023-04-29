import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.imageio.ImageIO;

public class Graphics implements BusListener {
    private Bus bus;

    public static final int SCREEN_WIDTH = 256;
    public static final int SCREEN_HEIGHT = 192;


    private static final int GREEN = (new Color(117, 250, 76)).getRGB(); 
    private static final int RED = (new Color(234, 50, 35)).getRGB(); 

    private int vramStartAddr;
    private int vramEndAddr;
    private AtomicIntegerArray vram;

    private BufferedImage img;

    private int[][][] charSet;

    public Graphics(Bus bus, int vramStartAddr, int vramEndAddr) {
        this.bus = bus;
        this.vramStartAddr = vramStartAddr;
        this.vramEndAddr = vramEndAddr;

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
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 32; col++) {
                int[][] character = charSet[vram.get(row * 32 + col)];
                // int[][] character = charSet[i % 256];
                // i++;

                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 12; y++) {
                        img.setRGB(col * 8 + x, row * 12 + y, character[y][x]);
                    }
                }
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
