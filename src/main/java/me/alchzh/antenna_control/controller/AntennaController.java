package me.alchzh.antenna_control.controller;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaEvent;
import me.alchzh.antenna_control.network.NetworkAntennaDevice;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static me.alchzh.antenna_control.util.Units.d;

/**
 * Controls a device by running scripts and outputting to log
 */
public class AntennaController {
    /**
     * The default log time format
     */
    public static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS ");

    private final AntennaDevice device;
    /**
     * The ExecutorService that spawns the script runner thread
     */
    private final ExecutorService es = Executors.newSingleThreadExecutor();
    /**
     * Future representing the running script
     */
    private Future<?> sf;
    /**
     * The baseTime is initially set to unix epoch before the device updates us
     */
    private ZonedDateTime baseTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));

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


    /**
     * @param device The device to control
     */
    public AntennaController(AntennaDevice device) {
        this.device = device;

        // Add our logging event listener
        device.addEventListener((AntennaEvent event) -> {
            String log = makeLogString(event);
            System.out.printf("%s %s\n", getFormattedTime(event.time), log);
        });
    }

    /**
     * Launches a controller with the configured AntennaDevice as a console application
     *
     * @param args Command line arguments
     * @throws IOException On any IOException
     */
    public static void main(String[] args) throws IOException {
//        AntennaDevice device = new MockAntennaDevice(0, 0, 0, 0, u(135), u(70), u(5 / 1000.0));
        AntennaDevice device = new NetworkAntennaDevice("127.0.0.1", 52532);

        AntennaController controller = new AntennaController(device);

        BufferedReader in
                = new BufferedReader(new FileReader("INPUT"));
        // AntennaScript can parse from any BufferedReader
        AntennaScript script = new AntennaScript(in);

        controller.runScript(script);
        System.exit(0);
    }

    /**
     * Properly formats the log output for every event type
     * Located under AntennaController as it is an logging implementation detail
     *
     * @param event The event to log
     * @return Formatted log output
     */
    private String makeLogString(AntennaEvent event) {
        ByteBuffer b = ByteBuffer.wrap(event.data);

        switch (event.type) {
            // Every case should end in a return; break is unnecessary
            case BASE_TIME:
                long millis = b.getLong();
                baseTime = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                return String.format("Set base time to %s", dtf.format(baseTime));
            case POSITION_UNIT_SIZE:
                // TODO: handle different position sizes
                assert b.get() == (byte) 0x04;
                return "Confirmed position size = 4";
            case CONTROL_SPEED:
                speed = b.getInt();
                return String.format("Set speed to %d u/ms = %.4f d/s", speed, d(speed) * 1000);
            case CONTROL_POSITION_RANGE:
                minAz = b.getInt();
                maxAz = b.getInt();
                minEl = b.getInt();
                maxEl = b.getInt();

                assert minAz < maxAz;
                assert minEl < maxEl;

                return String.format("Set minAz, maxAz, minEl, maxEl = %.3f, %.3f, %.3f, %.3f",
                        d(minAz), d(maxAz), d(minEl), d(maxEl));
            case CONTROL_BASE_POSITION:
                baseAz = b.getInt();
                baseEl = b.getInt();

                assert minAz < baseAz && baseAz < maxAz;
                assert minEl < baseEl && baseEl < maxEl;

                return String.format("Set base position to (%.3f, %.3f)", d(baseAz), d(baseEl));
            case COMMAND_ISSUED:
                return String.format("Issued command %s", AntennaCommand.readFromBuffer(b));
            case CURRENT_STATE:
                az = b.getInt();
                el = b.getInt();
                destAz = b.getInt();
                destEl = b.getInt();

                return String.format("Current location: (%.3f, %.3f)", d(az), d(el));
            case MOVE_FINISHED:
                az = b.getInt();
                el = b.getInt();

                return String.format("Move finished. Current location: (%.3f, %.3f)", d(az), d(el));
            default:
                // Prepend "Error: " to any errors so we don't need to redefine this every time
                return (event.isError() ? "Error: " : "") + event;
        }
    }

    /**
     * Formats a given time in milliseconds after baseTime
     *
     * @param time Time in milliseconds after the baseTime
     * @return Formatted time string
     */
    private String getFormattedTime(int time) {
        return dtf.format(baseTime.plus(Duration.ofMillis(time)));
    }

    /**
     * Runs script in thread. Power on the device before the script runs. Script itself doesn't necessarily
     * have to begin from a base state.
     *
     * @param script A script to run.
     */
    public void runScript(AntennaScript script) {
        poweron();
        sf = es.submit(script.attach(device));
        try {
            Object result = sf.get();
            System.out.println(result);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // For testing only. Remove for actual application.
        poweroff();
    }

    /**
     * Stop a currently running script immediately
     */
    public void cancelScript() {
        sf.cancel(true);
    }

    /**
     * Power on the device
     */
    public void poweron() {
        device.submitCommand(AntennaCommand.Type.POWERON);
    }

    /**
     * Power off the device
     */
    public void poweroff() {
        device.submitCommand(AntennaCommand.Type.POWEROFF);
    }
}