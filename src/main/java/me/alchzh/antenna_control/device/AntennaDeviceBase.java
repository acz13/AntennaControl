package me.alchzh.antenna_control.device;

import java.util.HashSet;
import java.util.Set;

public abstract class AntennaDeviceBase implements AntennaDevice {
    private final Set<AntennaEventListener> listeners = new HashSet<>();

    protected void sendRaw(byte[] data) {
        sendEvent(AntennaEvent.fromArray(data));
    }

    /**
     * Sends a regular (data) event to every registered listener
     *
     * @param event Event to send
     */
    private void sendEvent(AntennaEvent event) {
        for (AntennaEventListener listener : listeners) {
            listener.eventOccurred(event);
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
