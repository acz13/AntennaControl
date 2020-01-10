package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static me.alchzh.antenna_control.util.Hex.bytesToHex;

public class AntennaEvent {
    public final Type type;

    public AntennaEvent(Type type, int time, byte[] data) {
        this.type = type;
        this.time = time;
        this.data = data;
    }

    public final int time;
    public final byte[] data;

    public AntennaEvent(Type type, int time, int... data) {
        this.type = type;
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

        return new AntennaEvent(Type.fromCode(code), time, data);
    }

    public byte[] toArray() {
        ByteBuffer b = ByteBuffer.allocate(5 + data.length);

        b.put(type.getCode());
        b.putInt(time);
        b.put(data);

        return b.array();
    }

    @Override
    public String toString() {
        return "AntennaEvent{" +
                "type=" + type +
                ", time=" + time + "ms" +
                ", data=" + bytesToHex(data) +
                '}';
    }

    public boolean isError() {
        return type.getCode() >= 0x6F;
    }

    public enum Type {
        /* CONTROL EVENTS */
        POSITION_UNIT_SIZE(0x40),
        CONTROL_SPEED(0x41),
        CONTROL_POSITION_RANGE(0x42),
        CONTROL_BASE_POSITION(0x43),
        BASE_TIME(0x44),

        /* RESPONSE EVENTS */
        COMMAND_ISSUED(0x50),
        CURRENT_STATE(0x51),
        MOVE_FINISHED(0x52),
        MOVE_CANCELED(0x53),
        MEASUREMENT(0x60),

        /* ERROR EVENTS */
        PHYSICAL_POSITION_ERROR(0x70),
        DATA_ACQUISITION_ERROR(0x71),
        UNKNOWN_COMMAND_ERROR(0x72),
        DEVICE_POWEROFF_ERROR(0x79);

        private static final Map<Byte, Type> codeToTypeMap = new HashMap<>();

        static {
            for (Type type : Type.values()) {
                codeToTypeMap.put(type.code, type);
            }
        }

        private byte code;

        Type(int code) {
            this.code = (byte) code;
        }

        public static AntennaEvent.Type fromCode(byte code) {
            return codeToTypeMap.get(code);
        }

        public byte getCode() {
            return code;
        }
    }
}
