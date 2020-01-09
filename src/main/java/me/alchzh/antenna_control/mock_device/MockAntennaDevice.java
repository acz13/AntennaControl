package me.alchzh.antenna_control.mock_device;

import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaDeviceBase;
import me.alchzh.antenna_control.device.AntennaEvent;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A mock implementation of AntennaDeviceInterface with position size 4 (stored in an int)
 */
public class MockAntennaDevice extends AntennaDeviceBase implements AntennaDevice {
    private ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> sf;

    public static final double DRIFT_FACTOR = ((double) Integer.MAX_VALUE) / (180 * 240));

    private boolean poweredOn = false;

    private long baseSysTime;
    private byte[] baseSysTimeBA;
    private long baseNanoTime;

    private long moveStartTime = -1;
    private long moveTime = -1;
    private long moveFinishedTime = -1;
    private boolean tracking = false;
    private int az;
    private int el;
    private int startAz;
    private int startEl;
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

    private int getTimeElapsed() {
        return (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - baseNanoTime);
    }

    @Override
    public void submitCommand(byte[] data) {
        ByteBuffer b = ByteBuffer.wrap(data);
        byte command = b.get();

        if (command != POWERON) {
            if (!poweredOn) {
                return;
            }
            send(COMMAND_ISSUED, data);
            sendState();
        }

        switch (command) {
            case POWERON:
                poweredOn = true;
                baseSysTime = System.currentTimeMillis();
                baseNanoTime = System.nanoTime();

                ByteBuffer tb = ByteBuffer.allocate(8);
                tb.putLong(baseSysTime);
                baseSysTimeBA = tb.array();

                sendControlInfo();
                sendState();
                break;
            case G0:
                if (!Objects.isNull(sf)) {
                    sf.cancel(false);
                }

                startAz = az;
                startEl = el; 
                destAz = b.getInt();
                destEl = b.getInt();

                int azDist = Math.abs(destAz - startAz);
                int elDist = Math.abs(destEl - startEl);

                moveTime = Math.max(azDist, elDist) / speed;
                sf = ses.schedule(() -> {
                    az = destAz;
                    el = destEl;
                    tracking = true;
                    moveStartTime = -1;
                    moveTime = -1;
                    moveFinishedTime = System.nanoTime();
                    send(MOVE_FINISHED, destAz, destEl);
                }, moveTime, TimeUnit.MILLISECONDS);

                moveStartTime = System.nanoTime();
                break;
            default:
                send(UNKNOWN_COMMAND_ERROR, data[0]);
                break;
        }
    }

    private void updatePos() {
        if (moveStartTime == -1 && tracking) {
            long timeDelta = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - moveFinishedTime);

            az = destAz + (int)(timeDelta * DRIFT_FACTOR);
        } else if (moveStartTime > 0) {
            long timeDelta = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - moveStartTime);

            if (timeDelta > moveTime) {
                az = destAz;
                el = destEl;
            }

            az = Math.max(az + (int) timeDelta * speed, destAz);
        }
    }

    private void sendState() {
        send(CURRENT_STATE, az, el, destAz, destEl);
    }

    private void sendControlInfo() {
        send(BASE_TIME, baseSysTimeBA);
        send(POSITION_UNIT_SIZE, (byte) 0x04);
        send(CONTROL_SPEED, speed);
        send(CONTROL_POSITION_RANGE, minAz, maxAz, minEl, maxEl);
        send(CONTROL_BASE_POSITION, baseAz, baseEl);
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
