package me.alchzh.antenna_control.device;

import java.util.HashSet;
import java.util.Set;

public abstract class AntennaDeviceBase implements AntennaDevice {
    private final Set<AntennaEventListener> listeners = new HashSet<>();

    protected void sendRaw(byte[] data) {
        AntennaEvent event = AntennaEvent.fromArray(data);

        if (event.isError()) {
            sendErrorEvent(event);
        } else {
            sendDataEvent(event);
        }
    }

    /**
     * @param data Sends an error event to every registered listener raw
     */
    private void sendErrorEvent(AntennaEvent event) {
        for (AntennaEventListener listener : listeners) {
            listener.errorEventOccurred(event);
        }
    }

    /**
     * @param data Sends a data event to every registered listener raw
     */
    private void sendDataEvent(AntennaEvent event) {
        for (AntennaEventListener listener : listeners) {
            listener.dataEventOccurred(event);
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
