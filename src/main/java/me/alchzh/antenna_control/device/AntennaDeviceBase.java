package me.alchzh.antenna_control.device;

import java.util.HashSet;
import java.util.Set;

public abstract class AntennaDeviceBase implements AntennaDevice {
    private final Set<AntennaEventListener> listeners = new HashSet<>();

    /**
     * @param data Sends an error event to every registered listener raw
     */
    protected void sendError(byte[] data) {
        for (AntennaEventListener listener : listeners) {
            listener.errorEventOccurred(data);
        }
    }

    /**
     * @param data Sends a data event to every registered listener raw
     */
    protected void sendData(byte[] data) {
        for (AntennaEventListener listener : listeners) {
            listener.dataEventOccurred(data);
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
