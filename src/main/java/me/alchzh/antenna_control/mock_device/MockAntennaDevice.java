package me.alchzh.antenna_control.mock_device;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDeviceBase;
import me.alchzh.antenna_control.device.AntennaEvent;

public class MockAntennaDevice extends AntennaDeviceBase implements MockAntenna.Forwarder {
    private final MockAntenna antenna;

    public MockAntennaDevice(int baseAz, int baseEl, int minAz, int minEl, int maxAz, int maxEl, int speed) {
        antenna = new MockAntenna(this, baseAz, baseEl, minAz, minEl, maxAz, maxEl, speed);
    }

    @Override
    public void submitCommand(AntennaCommand command) {
        antenna.commandReceived(command);
    }

    @Override
    public void sendEvent(AntennaEvent event) {
        super.sendEvent(event);
    }
}
