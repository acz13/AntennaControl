package me.alchzh.antenna_control.controller;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaEvent;
import me.alchzh.antenna_control.mock_device.MockAntennaDevice;

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

class AntennaController {
    public static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS ");
    private final AntennaDevice device;
    private final ExecutorService es = Executors.newSingleThreadExecutor();
    private ZonedDateTime baseTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
    private Future<?> sf;

    private int speed;

    public AntennaController(AntennaDevice device) {
        this.device = device;

        device.addEventListener((AntennaEvent event) -> {
            String log = process(event);
            System.out.printf("%s %s\n", getFormattedTime(event.time), log);
        });
    }

    public static int u(double degrees) {
        return (int) (degrees * (Integer.MAX_VALUE / 180.0));
    }

    public static double d(int units) {
        return (double) units * 180.0 / Integer.MAX_VALUE;
    }

    public static void main(String[] args) throws IOException {
        AntennaDevice device = new MockAntennaDevice(0, 0, 0, 0, u(135), u(70), u(5 / 1000.0));
        AntennaController controller = new AntennaController(device);

        BufferedReader in
                = new BufferedReader(new FileReader("INPUT"));
        AntennaScript script = new AntennaScript(in);

        controller.runScript(script);
        System.exit(0);
    }

    private String process(AntennaEvent event) {
        ByteBuffer b = ByteBuffer.wrap(event.data);

        switch (event.type) {
            case BASE_TIME:
                long millis = b.getLong();
                baseTime = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                return "Set base time to " + dtf.format(baseTime);
            case POSITION_UNIT_SIZE:
                // TODO: handle different position sizes
                assert b.get() == (byte) 0x04;
                return "Confirmed position size = 4";
            case CONTROL_SPEED:
                speed = b.getInt();
                return "Set speed to " + speed + " u/ms = " + d(speed) * 1000 + " d/s";
        }

        return event.toString();
    }

    private String getFormattedTime(int time) {
        return dtf.format(baseTime.plus(Duration.ofMillis(time)));
    }

    public void runScript(AntennaScript script) {
        poweron();
        sf = es.submit(script.attach(device));
        try {
            sf.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        poweroff();
    }

    public void cancelScript() {
        sf.cancel(true);
    }

    public void poweron() {
        device.submitCommand(AntennaCommand.Type.POWERON);
    }

    public void poweroff() {
        device.submitCommand(AntennaCommand.Type.POWEROFF);
    }
}