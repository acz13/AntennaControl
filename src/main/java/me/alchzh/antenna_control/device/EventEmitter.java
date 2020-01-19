package me.alchzh.antenna_control.device;

public interface EventEmitter<T> {
    /**
     * Registers an event listener to the device.
     *
     * @param listener The listener
     */
    void addEventListener(Listener<T> listener);

    /**
     * Unregisters an event listener from the device.
     *
     * @param listener The listener
     */
    void removeEventListener(Listener<T> listener);

    /**
     * A listener for the event (registered with addEventListener) that can be used with lambdas
     */
    @FunctionalInterface
    interface Listener<T> {
        /**
         * Called when a regular (data) event is received
         *
         * @param event The event
         */
        void eventOccurred(T event);
    }
}
