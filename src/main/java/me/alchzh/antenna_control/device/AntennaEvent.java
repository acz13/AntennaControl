package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static me.alchzh.antenna_control.util.Hex.bytesToHex;

/**
 * Represents a events sent from a device to a listener (or passed from a device to another device)
 * Events must have a timeStamp in milliseconds from a baseTime (or epoch)
 */
public class AntennaEvent {
    public final Type type;
    public final int time;
    public final byte[] data;

    /**
     * Basic event creation from type, time, and array of bytes
     *
     * @param type The type of the event
     * @param time The time of the event (from baseTime)
     * @param data Byte array data / body / arguments
     */
    public AntennaEvent(Type type, int time, byte[] data) {
        this.type = type;
        this.time = time;

        assert type.getLength() == -1 || type.getLength() == data.length;
        this.data = data;
    }

    /**
     * Basic event creation from type, time, and array of integers
     *
     * @param type The type of the event
     * @param time The time of the event (from baseTime)
     * @param data Integer array data / body / arguments
     */
    public AntennaEvent(Type type, int time, int... data) {
        this.type = type;
        this.time = time;

        assert type.getLength() == data.length * Integer.BYTES;

        ByteBuffer d = ByteBuffer.allocate(type.getLength());
        for (int arg : data) {
            d.putInt(arg);
        }

        this.data = d.array();
    }

    /**
     * Reads the event from a buffer starting with the code. Reads only as far as specified by the command
     *
     * @param b Buffer
     * @return The first event found in the byte buffer
     */
    public static AntennaEvent readFromBuffer(ByteBuffer b) {
        byte code = b.get();
        int time = b.getInt();

        Type type = Type.fromCode(code);
        int length = type.getLength();
        byte[] data;

        if (length == -1) {
            byte code2 = b.get();
            int code2length = Type.fromCode(code2).getLength();
            data = new byte[1 + code2length];
            data[0] = code2;
            b.get(data, 1, code2length);
        } else {
            data = new byte[length];
            b.get(data);
        }

        return new AntennaEvent(type, time, data);
    }

    /**
     * Get byte buffer representation of the command
     *
     * @return byte buffer representation of the command
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer b = ByteBuffer.allocate(5 + data.length);

        b.put(type.getCode());
        b.putInt(time);
        b.put(data);
        b.flip();

        return b;
    }

    /**
     * Get byte array representation of the command
     *
     * @return byte array representation of the command
     */
    public byte[] toArray() {
        return toByteBuffer().array();
    }

    @Override
    public String toString() {
        return "AntennaEvent{" +
                "type=" + type +
                ", time=" + time + "ms" +
                ", data=" + bytesToHex(data) +
                '}';
    }

    /**
     * @return Detects if event is an error. For now, simply checks if event is 0x7X
     */
    public boolean isError() {
        return type.getCode() > 0x6F;
    }

    /**
     * Types of Events stored by their respective protocol code and length of arguments accepted in bytes
     */
    public enum Type {
        /* CONTROL EVENTS */
        POSITION_UNIT_SIZE(0x40, Byte.BYTES),
        CONTROL_SPEED(0x41, Integer.BYTES),
        CONTROL_POSITION_RANGE(0x42, 4 * Integer.BYTES),
        CONTROL_BASE_POSITION(0x43, 2 * Integer.BYTES),
        BASE_TIME(0x44, Long.BYTES),

        /* RESPONSE EVENTS */
        COMMAND_ISSUED(0x50, -1),
        CURRENT_STATE(0x51, 4 * Integer.BYTES),
        MOVE_FINISHED(0x52, 4 * Integer.BYTES),
        MOVE_CANCELED(0x53, 4 * Integer.BYTES),
        MEASUREMENT(0x60, -1),

        /* ERROR EVENTS */
        PHYSICAL_POSITION_ERROR(0x70, 0),
        DATA_ACQUISITION_ERROR(0x71, 0),
        UNKNOWN_COMMAND_ERROR(0x72, 1),
        DEVICE_POWEROFF_ERROR(0x79, 0);

        private static final Map<Byte, Type> codeToTypeMap = new HashMap<>();

        static {
            for (Type type : Type.values()) {
                codeToTypeMap.put(type.code, type);
            }
        }

        private byte code;
        private int length;

        Type(int code, int length) {
            this.code = (byte) code;
            this.length = length;
        }

        /**
         * Looks up event Type from code byte
         *
         * @param code Code byte to lookup
         * @return Type code corresponds to
         */
        public static AntennaEvent.Type fromCode(byte code) {
            return codeToTypeMap.get(code);
        }

        public byte getCode() {
            return code;
        }

        public int getLength() {
            return length;
        }
    }
}
