package me.alchzh.antenna_control.device;

import java.util.HashSet;
import java.util.Set;

public abstract class AntennaDeviceBase implements AntennaDevice {
    private final Set<AntennaEventListener> listeners = new HashSet<>();

    protected void sendRaw(byte[] data) {
        if (data[0] < 0x70) {
            sendRawData(data);
        } else {
            sendRawError(data);
        }
    }

    /**
     * @param data Sends an error event to every registered listener raw
     */
    private void sendRawError(byte[] data) {
        for (AntennaEventListener listener : listeners) {
            listener.errorEventOccurred(AntennaEvent.fromArray(data));
        }
    }

    /**
     * @param data Sends a data event to every registered listener raw
     */
    private void sendRawData(byte[] data) {
        for (AntennaEventListener listener : listeners) {
            listener.dataEventOccurred(AntennaEvent.fromArray(data));
        }
    }

    @Override
    public void addEventListener(AntennaEventListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeEventListener(AntennaEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
}
