import java.util.Map;
import java.util.HashMap;

import java.awt.event.KeyEvent;
import java.security.Key;

public class PS2 {

    private static final int[] KEY_CODES = {
        KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_E, KeyEvent.VK_F, KeyEvent.VK_G, KeyEvent.VK_H, KeyEvent.VK_I,
        KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L, KeyEvent.VK_M, KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_P, KeyEvent.VK_Q, KeyEvent.VK_R,
        KeyEvent.VK_S, KeyEvent.VK_T, KeyEvent.VK_U, KeyEvent.VK_V, KeyEvent.VK_W, KeyEvent.VK_X, KeyEvent.VK_Y, KeyEvent.VK_Z,
        KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,
        KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_TAB,
        KeyEvent.VK_SHIFT, KeyEvent.VK_ESCAPE, KeyEvent.VK_COMMA, KeyEvent.VK_SEMICOLON, KeyEvent.VK_PERIOD, KeyEvent.VK_SLASH, KeyEvent.VK_QUOTE
    };

    private static final int[] EXTENDED_KEY_CODES = {
        KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, 
    };

    private static final int[] SCAN_CODES = {
        0x1C, 0x32, 0x21, 0x23, 0x24, 0x2B, 0x34, 0x33, 0x43, 
        0x3B, 0x42, 0x4B, 0x3A, 0x31, 0x44, 0x4D, 0x15, 0x2D,
        0x1B, 0x2C, 0x3C, 0x2A, 0x1D, 0x22, 0x35, 0x1A, 
        0x45, 0x16, 0x1E, 0x26, 0x25, 0x2E, 0x36, 0x3D, 0x3E, 0x46,
        0x29, 0x5A, 0x66, 0x0D,
        0x12, 0x76, 0x41, 0x4C, 0x49, 0x4A, 0x52,
    };

    private static final int[] EXTENDED_SCAN_CODES = {
        0x75, 0x72, 0x6B, 0x74
    };

    private static Map<Integer, Integer> scanCodeMap; 

    public static final int EXTENDED_CODE = 0xE0;
    public static final int RELEASE_CODE = 0xF0;
    public static final int ERROR_CODE = 0x00;


    static {
        scanCodeMap = new HashMap<>();
        for (int i = 0; i < KEY_CODES.length; i++) {
            scanCodeMap.put(KEY_CODES[i], SCAN_CODES[i]);
        }

        for (int i = 0; i < EXTENDED_KEY_CODES.length; i++) {
            scanCodeMap.put(EXTENDED_KEY_CODES[i], EXTENDED_SCAN_CODES[i]);
        }
    }
    

    public static int getScanCode(int keyCode) {
        if (!scanCodeMap.containsKey(keyCode)) {
            return ERROR_CODE;
        }
        return scanCodeMap.get(keyCode);
    }

    public static boolean isExtended(int keyCode) {
        for (int i = 0; i < EXTENDED_KEY_CODES.length; i++) {
            if (keyCode == EXTENDED_KEY_CODES[i]) {
                return true;
            }
        }

        return false;
    }
}
