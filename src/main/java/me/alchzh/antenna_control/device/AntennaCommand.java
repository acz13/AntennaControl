package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static me.alchzh.antenna_control.util.Hex.bytesToHex;

/**
 * Represents a command sent from a controller to a device (or passed from a device to another device)
 * Commands have no timestamp, only a type and arguments in Data.
 */
public class AntennaCommand {
    public final Type type;
    public final byte[] data;

    /**
     * Basic command creation from type and data
     *
     * @param type The type of the command
     * @param data Command data / body / arguments
     */
    public AntennaCommand(Type type, byte[] data) {
        this.type = type;

        // Ensure the length of data
        assert type.getLength() == -1 || type.getLength() == data.length;
        this.data = data;
    }

    /**
     * Basic command creation from type and array of integers
     *
     * @param type The type of the command
     * @param data Integer array data / body / arguments
     */
    public AntennaCommand(AntennaCommand.Type type, int... data) {
        this.type = type;

        assert type.getLength() == data.length * Integer.BYTES;

        ByteBuffer d = ByteBuffer.allocate(type.getLength());
        for (int arg : data) {
            d.putInt(arg);
        }

        this.data = d.array();
    }

    /**
     * Reads the command from a buffer starting with the code. Reads only as far as specified by the command
     *
     * @param b Buffer
     * @return The first command found in the byte buffer
     */
    public static AntennaCommand readFromBuffer(ByteBuffer b) {
        byte code = b.get();

        AntennaCommand.Type type = AntennaCommand.Type.fromCode(code);
        int length = type.getLength();
        byte[] data;

        if (length == -1) {
            // If length unspecified, get new code from next byte
            byte code2 = b.get();
            int code2length = AntennaCommand.Type.fromCode(code2).getLength();
            data = new byte[1 + code2length];
            data[0] = code2;
            // b.get(arr, offset, length) let's us read starting from index 1
            b.get(data, 1, code2length);
        } else {
            data = new byte[length];
            b.get(data);
        }

        return new AntennaCommand(AntennaCommand.Type.fromCode(code), data);
    }

    /**
     * Get byte buffer representation of the command
     *
     * @return byte buffer representation of the command
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer b = ByteBuffer.allocate(1 + data.length);

        b.put(type.getCode());
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
        return "AntennaCommand{" +
                "type=" + type +
                ", data=" + bytesToHex(data) +
                '}';
    }


    /**
     * Types of Commands stored by their respective protocol code and length of arguments accepted in bytes
     */
    public enum Type {
        /* COMMANDS */
        G0(0x01, 8),
        T0(0x03, 1),
        A0(0x05, 1),
        POWERON(0x08, 0),
        POWEROFF(0x09, 0);

        private static final Map<Byte, AntennaCommand.Type> codeToTypeMap = new HashMap<>();

        static {
            for (AntennaCommand.Type type : AntennaCommand.Type.values()) {
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
         * Looks up command Type from code byte
         *
         * @param code Code byte to lookup
         * @return Type code corresponds to
         */
        public static AntennaCommand.Type fromCode(byte code) {
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
