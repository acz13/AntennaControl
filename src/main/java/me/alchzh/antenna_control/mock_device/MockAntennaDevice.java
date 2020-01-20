package me.alchzh.antenna_control.mock_device;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.EventEmitterImpl;
import me.alchzh.antenna_control.device.AntennaEvent;

import java.nio.ByteBuffer;
import java.util.concurrent.*;

/**
 * Mocks a real Antenna device for testing
 */
public class MockAntennaDevice extends EventEmitterImpl<AntennaEvent> implements AntennaDevice {
    /**
     * The speed (in units / millisecond) that the sky drifts at
     */
    public static final double DRIFT_FACTOR = ((double) Integer.MAX_VALUE) / (180 * 240000);
    private final int baseAz;
    private final int baseEl;
    private final int minAz;
    private final int minEl;
    private final int maxAz;
    private final int maxEl;
    private final int speed;

    private int sensorAz;
    private int sensorEl;
    private final int sensorInterval = 5000;
    private final int sensorCount = 96;
    private final MockDataGenerator sensorDataGen = new MockDataGenerator(120, 5, 8);

    private ScheduledExecutorService ses;
    private ScheduledFuture<?> sendStateSF;
    private ScheduledFuture<?> dataCollectSF;
    private boolean poweredOn = false;
    private long baseNanoTime;
    private long moveStartTime = -1;
    private long moveTime = -1;
    private long moveFinishedTime = -1;
    private boolean tracking = false;
    private long trackingStartTime = -1;
    private long lastTrackTime;
    private int az;
    private int el;
    private int startAz;
    private int startEl;
    private int destAz;
    private int destEl;
    //    private long baseSysTime;
    private byte[] baseSysTimeBA;

    private final Object COMMAND_MONITOR = new Object();

    /**
     * @param baseAz Base azimuth. The antenna starts at the base position and the STOW command returns it to the base position
     * @param baseEl Base elevation. The antenna starts at the base position and the STOW command returns it to the base position
     * @param minAz  Minimum azimuth. For now, must be signed lower than max azimuth.
     * @param minEl  Minimum elevation. For now, must be signed lower than max elevation.
     * @param maxAz  Maximum azimuth. For now, must be signed higher than min azimuth.
     * @param maxEl  Maximum elevation. For now, must be signed lower than min elevation.
     * @param speed  The speed in units per millisecond of the antenna
     */
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
        synchronized (COMMAND_MONITOR) {
            if (command.type != AntennaCommand.Type.POWERON) {
                if (!poweredOn) {
                    send(AntennaEvent.Type.DEVICE_POWEROFF_ERROR);
                    return;
                }
                sendState();
                send(AntennaEvent.Type.COMMAND_ISSUED, command.toArray());
            }

            ByteBuffer b = ByteBuffer.wrap(command.data);

            switch (command.type) {
                case POWERON:
                    if (ses == null || ses.isShutdown()) {
                        ses = Executors.newScheduledThreadPool(
                                2, r -> new Thread(r, "mockAntennaDevice")
                        );
                        ((ScheduledThreadPoolExecutor) ses).setRemoveOnCancelPolicy(true);
                    }

                    poweredOn = true;
                    long baseSysTime = System.currentTimeMillis();
                    baseNanoTime = System.nanoTime();
                    moveFinishedTime = baseNanoTime;

                    ByteBuffer tb = ByteBuffer.allocate(8);
                    tb.putLong(baseSysTime);
                    baseSysTimeBA = tb.array();

                    sendControlInfo();
                    sendStateSF = ses.scheduleAtFixedRate(this::sendState, 0, 1000, TimeUnit.MILLISECONDS);

                    break;
                case POWEROFF:
                    poweredOn = false;
                    sendStateSF.cancel(true);
                    ses.shutdownNow();
                    break;
                case G0:
                    updatePos();

                    if (moveStartTime > 0) {
                        send(AntennaEvent.Type.MOVE_CANCELED, az, el, destAz, destEl);
                    }

                    startAz = az;
                    startEl = el;

                    int adjust = tracking ? (int) (DRIFT_FACTOR * getTimeElapsed(trackingStartTime))
                            : 0;

                    destAz = b.getInt() + adjust;
                    destEl = b.getInt();

                    int azDist = Math.abs(destAz - startAz);
                    int elDist = Math.abs(destEl - startEl);

                    moveTime = Math.max(azDist, elDist) / speed;
                    moveStartTime = System.nanoTime();
                    break;
                case T0:
                    tracking = b.get() != 0;
                    trackingStartTime = moveFinishedTime;
                    lastTrackTime = trackingStartTime;
                    break;
                case A0:
                    boolean on = b.get() != 0;
                    if (on) {
                        if (dataCollectSF == null || dataCollectSF.isCancelled() || dataCollectSF.isDone()) {
                            dataCollectSF = ses.scheduleAtFixedRate(this::sendData, 0, 5000, TimeUnit.MILLISECONDS);
                        } else {
                            dataCollectSF.cancel(true);
                        }
                    }
                    break;
                default:
                    send(AntennaEvent.Type.UNKNOWN_COMMAND_ERROR, command.type.getCode());
                    break;
            }
        }
    }

    private void updatePos() {
        if (tracking) {
            int adjust = (int) (DRIFT_FACTOR * getTimeElapsed(lastTrackTime));
            lastTrackTime = System.nanoTime();

            startAz += adjust;
            sensorAz += adjust;
            az += adjust;
            destAz += adjust;
        }

        if (moveStartTime > 0) {
            long timeDelta = getTimeElapsed(moveStartTime);

            if (timeDelta >= moveTime) {
                az = destAz;
                el = destEl;

                moveStartTime = -1;
                moveTime = -1;
                moveFinishedTime = System.nanoTime();
                send(AntennaEvent.Type.MOVE_FINISHED, destAz, destEl);
            } else {
                if (az < destAz) {
                    az = Math.min(startAz + (int) timeDelta * speed, destAz);
                } else {
                    az = Math.max(startAz - (int) timeDelta * speed, destAz);
                }

                if (el < destEl) {
                    el = Math.min(startEl + (int) timeDelta * speed, destEl);
                } else {
                    el = Math.max(startEl - (int) timeDelta * speed, destEl);
                }
            }
        }

        // TODO: Work properly when min > max (i.e. crosses 180 degrees)
        if (az < minAz || az > maxAz || el < minEl || el > maxEl) {
            send(AntennaEvent.Type.PHYSICAL_POSITION_ERROR);
        }
    }

    private void sendState() {
        try {
            updatePos();
            send(AntennaEvent.Type.CURRENT_STATE, az, el, destAz, destEl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendData() {
        try {
            sendState();

            if (az != sensorAz || el != sensorEl) {
                sensorDataGen.getNewMean();
                sensorAz = az;
                sensorEl = el;
            }

            ByteBuffer measurementBuffer = ByteBuffer.allocate(Integer.BYTES + sensorCount * Float.BYTES);
            measurementBuffer.putInt(sensorCount);
            for (int i = 0; i < sensorCount; i++) {
                measurementBuffer.putFloat(sensorDataGen.collectData());
            }

            measurementBuffer.flip();
            send(AntennaEvent.Type.MEASUREMENT, measurementBuffer.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendControlInfo() {
        send(AntennaEvent.Type.BASE_TIME, baseSysTimeBA);
        send(AntennaEvent.Type.POSITION_UNIT_SIZE, (byte) 0x04);
        send(AntennaEvent.Type.CONTROL_SPEED, speed);
        send(AntennaEvent.Type.CONTROL_POSITION_RANGE, minAz, maxAz, minEl, maxEl);
        send(AntennaEvent.Type.CONTROL_BASE_POSITION, baseAz, baseEl);
    }

    /**
     * @param data Sends properly packed data event to every registered listener
     */
    private void send(AntennaEvent.Type eventType, byte... data) {
        sendEvent(new AntennaEvent(eventType, getTimeElapsed(), data));
    }

    /**
     * @param data Sends properly packed data event to every registered listener
     */
    private void send(AntennaEvent.Type eventType, int... data) {
        sendEvent(new AntennaEvent(eventType, getTimeElapsed(), data));
    }
}
