package me.alchzh.antenna_control.util;

/**
 * Utility class for displaying bytes as hexadecimal codes
 */
public class Hex {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private Hex() {
    }

    /**
     * Represent an array of bytes in hexadecimal
     *
     * @param arr Byte array to convert
     * @return Hexadecimal string representation of array
     */
    public static String bytesToHex(byte[] arr) {
        char[] hexChars = new char[arr.length * 2];
        for (int j = 0; j < arr.length; j++) {
            int v = arr[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Represent a byte in hexadecimal
     *
     * @param b Byte to convert
     * @return Hexadecimal string representation of byte
     */
    public static String byteToHex(byte b) {
        int v = b & 0xFF;

        char[] hexChars = new char[]{HEX_ARRAY[v >>> 4], HEX_ARRAY[v & 0x0F]};

        return new String(hexChars);
    }
}
