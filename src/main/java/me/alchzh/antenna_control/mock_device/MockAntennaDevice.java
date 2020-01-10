package me.alchzh.antenna_control.mock_device;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaDeviceBase;
import me.alchzh.antenna_control.device.AntennaEvent;

import java.nio.ByteBuffer;
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

    public static final double DRIFT_FACTOR = ((double) Integer.MAX_VALUE) / (180 * 240);

    private boolean poweredOn = false;
    private final int baseAz;
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
    private final int baseEl;
    private final int minAz;
    private final int minEl;
    private final int maxAz;
    private final int maxEl;
    private final int speed;
    //    private long baseSysTime;
    private byte[] baseSysTimeBA;

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

    private int getTimeElapsed() {
        return getTimeElapsed(baseNanoTime);
    }

    private int getTimeElapsed(long since) {
        return (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - since);
    }

    @Override
    public void submitCommand(AntennaCommand command) {
        if (command.code != AntennaCommand.POWERON) {
            if (!poweredOn) {
                send(AntennaEvent.DEVICE_POWEROFF_ERROR);
                return;
            }
            send(AntennaEvent.COMMAND_ISSUED, command.toArray());
            sendState();
        }

        ByteBuffer b = ByteBuffer.wrap(command.data);

        switch (command.code) {
            case AntennaCommand.POWERON:
                poweredOn = true;
                long baseSysTime = System.currentTimeMillis();
                baseNanoTime = System.nanoTime();

                ByteBuffer tb = ByteBuffer.allocate(8);
                tb.putLong(baseSysTime);
                baseSysTimeBA = tb.array();

                sendControlInfo();
                ses.scheduleAtFixedRate(this::sendState, 0, 1000, TimeUnit.MILLISECONDS);
                break;
            case AntennaCommand.POWEROFF:
                poweredOn = false;
                ses.shutdownNow();
                break;
            case AntennaCommand.G0:
                updatePos();

                if (moveStartTime > 0) {
                    send(AntennaEvent.MOVE_CANCELED, az, el, destAz, destEl);
                }

                startAz = az;
                startEl = el;
                destAz = b.getInt();
                destEl = b.getInt();

                int azDist = Math.abs(destAz - startAz);
                int elDist = Math.abs(destEl - startEl);

                moveTime = Math.max(azDist, elDist) / speed;
                moveStartTime = System.nanoTime();
                break;
            case AntennaCommand.T0:
                tracking = b.get() != 0;
                break;
            default:
                send(AntennaEvent.UNKNOWN_COMMAND_ERROR, command.code);
                break;
        }
    }

    private void updatePos() {
        if (moveStartTime == -1 && tracking) {
            long timeDelta = getTimeElapsed(moveFinishedTime);

            az = destAz + (int)(timeDelta * DRIFT_FACTOR);
        } else if (moveStartTime > 0) {
            long timeDelta = getTimeElapsed(moveStartTime);

            if (timeDelta >= moveTime) {
                az = destAz;
                el = destEl;

                moveStartTime = -1;
                moveTime = -1;
                moveFinishedTime = System.nanoTime();
                send(AntennaEvent.MOVE_FINISHED, destAz, destEl);
            } else {
                az = Math.max(startAz + (int) timeDelta * speed, destAz);
                el = Math.max(startEl + (int) timeDelta * speed, startEl);
            }
        }

        // TODO: Work properly when min > max (i.e. crosses 180 degrees)
        if (az < minAz || az > maxAz || el < minEl || el > maxEl) {
            send(AntennaEvent.PHYSICAL_POSITION_ERROR);
        }
    }

    private void sendState() {
        updatePos();
        send(AntennaEvent.CURRENT_STATE, az, el, destAz, destEl);
    }

    private void sendControlInfo() {
        send(AntennaEvent.BASE_TIME, baseSysTimeBA);
        send(AntennaEvent.POSITION_UNIT_SIZE, (byte) 0x04);
        send(AntennaEvent.CONTROL_SPEED, speed);
        send(AntennaEvent.CONTROL_POSITION_RANGE, minAz, maxAz, minEl, maxEl);
        send(AntennaEvent.CONTROL_BASE_POSITION, baseAz, baseEl);
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
