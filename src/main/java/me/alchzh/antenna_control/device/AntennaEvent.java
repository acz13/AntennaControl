package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;

import static me.alchzh.antenna_control.util.Hex.byteToHex;
import static me.alchzh.antenna_control.util.Hex.bytesToHex;

public class AntennaEvent {
    /* RESPONSE EVENT CODES */
    public static final byte POSITION_UNIT_SIZE = 0x40; // SIZE (BYTES) OF POSITION
    public static final byte CONTROL_SPEED = 0x41; // UNIT PER MILLIS
    public static final byte CONTROL_POSITION_RANGE = 0x42; // MIN_AZ MAX_AZ MIN_EL MAX_EL
    public static final byte CONTROL_BASE_POSITION = 0x43; // AZ EL
    public static final byte BASE_TIME = 0x44; // 8 BYTE UNIX TIME (LONG)
    public static final byte COMMAND_ISSUED = 0x50; // CMD <DATA>
    public static final byte CURRENT_STATE = 0x51; // AZ EL DEST_AZ DEST_EL
    public static final byte MOVE_FINISHED = 0x52; // AZ EL
    public static final byte MOVE_CANCELED = 0x53; // AZ EL DEST_AZ DEST_EL
    public static final byte MEASUREMENT = 0x60; // VALUE
    /* RESPONSE ERROR CODES */
    public static final byte PHYSICAL_POSITION_ERROR = 0x70;
    public static final byte DATA_ACQUISITION_ERROR = 0x71;
    public static final byte UNKNOWN_COMMAND_ERROR = 0x72;
    public static final byte DEVICE_POWEROFF_ERROR = 0x79;
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
