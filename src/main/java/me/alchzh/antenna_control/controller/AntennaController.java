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

    public AntennaController(AntennaDevice device) {
        this.device = device;

        device.addEventListener((AntennaEvent event) -> {
            if (event.type == AntennaEvent.Type.BASE_TIME) {
                long millis = ByteBuffer.wrap(event.data).getLong();
                baseTime =
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            }

            System.out.print(dtf.format(baseTime.plus(Duration.ofMillis(event.time))));

            if (event.isError()) System.out.print("Error ");
            System.out.println(event);
        });
    }

    public static int d(double degrees) {
        return (int) (degrees * (Integer.MAX_VALUE / 180.0));
    }

    public static void main(String[] args) throws IOException {
        AntennaDevice device = new MockAntennaDevice(0, 0, 0, 0, d(135), d(70), d(5 / 1000.0));
        AntennaController controller = new AntennaController(device);

        BufferedReader in
                = new BufferedReader(new FileReader("INPUT"));
        AntennaScript script = new AntennaScript(in);

        controller.runScript(script);
        System.exit(0);
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