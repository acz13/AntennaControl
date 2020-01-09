package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;

public class AntennaEvent {
    public final byte code;
    public final int time;
    public final byte[] data;

    public AntennaEvent(byte code, int time, byte[] data) {
        this.code = code;
        this.time = time;
        this.data = data;
    }


    public AntennaEvent(byte code, int time, int... data) {
        this.code = code;
        this.time = time;

        ByteBuffer d = ByteBuffer.allocate(data.length * (Integer.SIZE / Byte.SIZE));
        for (int arg : data) {
            d.putInt(arg);
        }

        this.data = d.array();
    }

    public static AntennaEvent fromArray(byte[] raw) {
        ByteBuffer b = ByteBuffer.wrap(raw);

        byte code = b.get();
        int time = b.getInt();
        byte[] data = new byte[b.remaining()];
        b.get(data);

        return new AntennaEvent(code, time, data);
    }

    public byte[] toArray() {
        ByteBuffer b = ByteBuffer.allocate(5 + data.length);

        b.put(code);
        b.putInt(time);
        b.put(data);

        return b.array();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String byteToHex(byte b) {
        int v = b & 0xFF;

        char[] hexChars = new char[]{ HEX_ARRAY[v >>> 4], HEX_ARRAY[v & 0x0F] };

        return new String(hexChars);
    }

    @Override
    public String toString() {
        return "AntennaEvent{" +
                "code=" + byteToHex(code) +
                ", time=" + time +
                ", data=" + bytesToHex(data) +
                '}';
    }

    public boolean isError() {
        return code >= 0x6F;
    }
}
