package me.alchzh.antenna_control.device;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AntennaDeviceBase implements AntennaDevice {
    private final Set<AntennaEventListener> listeners = new ConcurrentHashMap<>().newKeySet();

    protected void sendRaw(byte[] data) {
        sendEvent(AntennaEvent.fromArray(data));
    }

    /**
     * Sends a regular (data) event to every registered listener
     *
     * @param event Event to send
     */
    protected void sendEvent(AntennaEvent event) {
        for (AntennaEventListener listener : listeners) {
            listener.eventOccurred(event);
        }
    }

    @Override
    public void addEventListener(AntennaEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeEventListener(AntennaEventListener listener) {
        listeners.remove(listener);
    }
}
