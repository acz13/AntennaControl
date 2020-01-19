package me.alchzh.antenna_control.device;

import java.util.HashSet;
import java.util.Set;

/**
 * Thread safe event listener implementation of AntennaDevice
 */
public abstract class EventEmitterImpl<T> implements EventEmitter<T> {
    private final Object MONITOR = new Object();
    private Set<Listener<T>> listeners;

    /**
     * Sends a regular (data) event to every registered listener
     *
     * @param event Event to send
     */
    protected void sendEvent(T event) {
        Set<Listener<T>> listenersCopy;

        synchronized (MONITOR) {
            if (listeners == null) return;
            listenersCopy = new HashSet<>(listeners);
        }

        for (Listener<T> listener : listenersCopy) {
            listener.eventOccurred(event);
        }
    }

    @Override
    public void addEventListener(Listener<T> listener) {
        if (listener == null) return;

        synchronized (MONITOR) {
            if (listeners == null) {
                listeners = new HashSet<>(1);
            }

            listeners.add(listener);
        }
    }

    @Override
    public void removeEventListener(Listener<T> listener) {
        synchronized (MONITOR) {
            if (listeners != null) listeners.remove(listener);
        }
    }
}
