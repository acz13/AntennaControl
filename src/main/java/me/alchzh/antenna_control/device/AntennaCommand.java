package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static me.alchzh.antenna_control.util.Hex.bytesToHex;

public class AntennaCommand {
    public final Type type;

    public AntennaCommand(Type type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public final byte[] data;

    public AntennaCommand(Type type, int... data) {
        this.type = type;

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
        G0(0x01),
        T0(0x03),
        A0(0x05),
        POWERON(0x08),
        POWEROFF(0x09);

        private static final Map<Byte, AntennaCommand.Type> codeToTypeMap = new HashMap<>();

        static {
            for (AntennaCommand.Type type : AntennaCommand.Type.values()) {
                codeToTypeMap.put(type.code, type);
            }
        }

        private byte code;

        Type(int code) {
            this.code = (byte) code;
        }

        public static AntennaCommand.Type fromCode(byte code) {
            return codeToTypeMap.get(code);
        }

        public byte getCode() {
            return code;
        }
    }
}
