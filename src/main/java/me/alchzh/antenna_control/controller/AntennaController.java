package me.alchzh.antenna_control.controller;

import me.alchzh.antenna_control.device.*;
import me.alchzh.antenna_control.mock_device.MockAntennaDevice;

class AntennaController {
    private AntennaDevice device;
    private Thread mainThread;

    public static int d(double degrees) {
        return (int) ((degrees / 180) * Integer.MAX_VALUE);
    }

    public AntennaController(AntennaDevice device) {
        this.device = device;

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
    }

    public Thread run() {
        mainThread = new Thread(() -> {
            device.submitCommand(new byte[]{AntennaDevice.POWERON});

            try  {
                Thread.sleep(1000);
            } catch (InterruptedException ie)  {

            }

            device.submitCommand(new byte[]{AntennaDevice.G0});
        }, "mainThread");

        mainThread.start();
        return mainThread;
    }

    public Thread getMainThread() {
        return mainThread;
    }

    public static void main(String[] args) {
        AntennaDevice device = new MockAntennaDevice(0, 0, 0, 0, d(135), d(70), d(5));
        AntennaController controller = new AntennaController(device);

        controller.run();
    }
}