package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static me.alchzh.antenna_control.util.Hex.bytesToHex;

public class AntennaCommand {
    public final Type type;
    public final byte[] data;

    public AntennaCommand(Type type, byte[] data) {
        this.type = type;

        assert type.getLength() == -1 || type.getLength() == data.length;
        this.data = data;
    }

    public AntennaCommand(AntennaCommand.Type type, int... data) {
        this.type = type;

        assert type.getLength() == data.length * Integer.BYTES;

        ByteBuffer d = ByteBuffer.allocate(type.getLength());
        for (int arg : data) {
            d.putInt(arg);
        }

        this.data = d.array();
    }

    public static AntennaCommand readFromBuffer(ByteBuffer b) {
        byte code = b.get();

        AntennaCommand.Type type = AntennaCommand.Type.fromCode(code);
        int length = type.getLength();
        byte[] data;

        if (length == -1) {
            byte code2 = b.get();
            int code2length = AntennaCommand.Type.fromCode(code2).getLength();
            data = new byte[1 + code2length];
            data[0] = code2;
            b.get(data, 1, code2length);
        } else {
            data = new byte[length];
            b.get(data);
        }

        return new AntennaCommand(AntennaCommand.Type.fromCode(code), data);
    }

    public byte[] toArray() {
        ByteBuffer b = ByteBuffer.allocate(1 + data.length);

        b.put(type.getCode());
        b.put(data);

        return b.array();
    }

    @Override
    public String toString() {
        return "AntennaCommand{" +
                "type=" + type +
                ", data=" + bytesToHex(data) +
                '}';
    }


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
