package me.alchzh.antenna_control.mock_device;

import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaDeviceBase;
import me.alchzh.antenna_control.device.AntennaEvent;
import me.alchzh.antenna_control.device.AntennaEventListener;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * A mock implementation of AntennaDeviceInterface with position size 4 (stored in an int)
 */
public class MockAntennaDevice extends AntennaDeviceBase implements AntennaDevice {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private boolean poweredOn = false;
    private long baseSysTime;
    private byte[] baseSysTimeBA;
    private long baseNanoTime;
    private int az;
    private int el;
    private int destAz;
    private int destEl;
    private int baseAz;
    private int baseEl;
    private int minAz;
    private int minEl;
    private int maxAz;
    private int maxEl;
    private int speed;

    public MockAntennaDevice(int baseAz, int baseEl, int minAz, int minEl, int maxAz, int maxEl, int speed) {
        this.az = baseAz;
        this.el = baseEl;

        this.baseAz = baseAz;
        this.baseEl = baseEl;

        this.minAz = minAz;
        this.minEl = minEl;
        this.maxAz = maxAz;
        this.maxEl = maxEl;

        this.speed = speed;
    }

    public static int d(double degrees) {
        return (int) ((degrees / 180) * Integer.MAX_VALUE);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void main(String[] args) {
        AntennaDevice device = new MockAntennaDevice(0, 0, 0, 0, d(135), d(70), d(5));

        device.addEventListener(new AntennaEventListener() {
            @Override
            public void errorEventOccurred(AntennaEvent event) {
                System.out.print("Error ");
                System.out.println(event);
            }

            @Override
            public void dataEventOccurred(AntennaEvent event) {
                System.out.println(event);
            }
        });

        device.submitCommand(new byte[]{AntennaDevice.POWERON});
        device.submitCommand(new byte[]{AntennaDevice.G0});
    }

    private int getTimeElapsed() {
        return (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - baseNanoTime);
    }

    @Override
    public void submitCommand(byte[] data) {
        if (data[0] != POWERON) {
            if (!poweredOn) {
                return;
            }
            send(COMMAND_ISSUED, data);
            sendState();
        }

        switch (data[0]) {
            case POWERON:
                poweredOn = true;
                baseSysTime = System.currentTimeMillis();
                baseNanoTime = System.nanoTime();

                ByteBuffer b = ByteBuffer.allocate(8);
                b.putLong(baseSysTime);
                baseSysTimeBA = b.array();

                sendControlInfo();
                sendState();
                break;
            default:
                send(UNKNOWN_COMMAND_ERROR, data[0]);
                break;
        }
    }

    private void sendState() {
        send(CURRENT_STATE, az, el, destAz, destEl);
    }

    private void sendControlInfo() {
        send(POSITION_UNIT_SIZE, (byte) (Integer.SIZE / Byte.SIZE));
        send(CONTROL_SPEED, speed);
        send(CONTROL_POSITION_RANGE, minAz, maxAz, minEl, maxEl);
        send(CONTROL_BASE_POSITION, baseAz, baseEl);
        send(BASE_TIME, baseSysTimeBA);
    }

    /**
     * @param data Sends properly packed data event to every registered listener
     */
    private void send(byte eventCode, byte... data) {
        sendRaw((new AntennaEvent(eventCode, getTimeElapsed(), data)).toArray());
    }

    /**
     * @param data Sends properly packed data event to every registered listener
     */
    private void send(byte eventCode, int... data) {
        sendRaw((new AntennaEvent(eventCode, getTimeElapsed(), data)).toArray());
    }
}
