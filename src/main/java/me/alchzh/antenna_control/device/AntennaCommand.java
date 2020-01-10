package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;

import static me.alchzh.antenna_control.util.Hex.byteToHex;
import static me.alchzh.antenna_control.util.Hex.bytesToHex;

public class AntennaCommand {
    /* COMMAND CODES */
    public static final byte G0 = 0x01; // GOTO AZ EL
    public static final byte T0 = 0x03; // TRACKING ON OFF
    public static final byte A0 = 0x05; // DATA ACQUISITION ON OFF (1 OR 0)
    public static final byte POWERON = 0x08; // POWER ON FROM SLEEP
    public static final byte POWEROFF = 0x09; // POWER OFF

    public final byte code;
    public final byte[] data;

    public AntennaCommand(byte code, byte[] data) {
        this.code = code;
        this.data = data;
    }

    public AntennaCommand(byte code, int... data) {
        this.code = code;

        ByteBuffer d = ByteBuffer.allocate(data.length * (Integer.SIZE / Byte.SIZE));
        for (int arg : data) {
            d.putInt(arg);
        }

        this.data = d.array();
    }

    public static AntennaCommand fromArray(byte[] raw) {
        ByteBuffer b = ByteBuffer.wrap(raw);

        byte code = b.get();
        byte[] data = new byte[b.remaining()];
        b.get(data);

        return new AntennaCommand(code, data);
    }

    public byte[] toArray() {
        ByteBuffer b = ByteBuffer.allocate(1 + data.length);

        b.put(code);
        b.put(data);

        return b.array();
    }


    @Override
    public String toString() {
        return "AntennaCommand{" +
                "code=" + byteToHex(code) +
                ", data=" + bytesToHex(data) +
                '}';
    }
}
