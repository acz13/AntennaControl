package me.alchzh.antenna_control.device;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public abstract class AntennaDeviceBase implements AntennaDevice {
    private final Object MONITOR = new Object();
    private Set<AntennaDevice.Listener> listeners;

    protected void sendRaw(byte[] data) {
        sendEvent(AntennaEvent.readFromBuffer(ByteBuffer.wrap(data)));
    }

    /**
     * Sends a regular (data) event to every registered listener
     *
     * @param event Event to send
     */
    protected void sendEvent(AntennaEvent event) {
        Set<AntennaDevice.Listener> observersCopy;

        synchronized (MONITOR) {
            if (listeners == null) return;
            observersCopy = new HashSet<>(listeners);
        }

        for (Listener listener : observersCopy) {
            listener.eventOccurred(event);
        }
    }

    @Override
    public void addEventListener(AntennaDevice.Listener listener) {
        if (listener == null) return;

        synchronized (MONITOR) {
            if (listeners == null) {
                listeners = new HashSet<>(1);
            }

            listeners.add(listener);
        }
    }

    @Override
    public void removeEventListener(AntennaDevice.Listener listener) {
        synchronized (MONITOR) {
            if (listeners != null) listeners.remove(listener);
        }
    }
}
