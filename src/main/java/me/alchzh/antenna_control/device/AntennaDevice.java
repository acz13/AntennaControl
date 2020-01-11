package me.alchzh.antenna_control.device;

public interface AntennaDevice {
    /**
     * Submits a command to the device
     *
     * @param command Command to submit
     */
    void submitCommand(AntennaCommand command);

    /**
     * Submits a command to the device
     *
     * @param type Command type
     * @param data Command data / arguments
     */
    default void submitCommand(AntennaCommand.Type type, byte... data) {
        submitCommand(new AntennaCommand(type, data));
    }

    /**
     * Submits a command to the device
     *
     * @param type Command type
     * @param data Command data / arguments
     */
    default void submitCommand(AntennaCommand.Type type, int... data) {
        submitCommand(new AntennaCommand(type, data));
    }

    /**
     * Registers an event listener to the device.
     *
     * @param listener The listener
     */
    void addEventListener(AntennaDevice.Listener listener);

    /**
     * Unregisters an event listener from the device.
     *
     * @param listener The listener
     */
    void removeEventListener(AntennaDevice.Listener listener);

    @FunctionalInterface
    interface Listener {
        /**
         * Called when a regular (data) event is received
         *
         * @param event The event
         */
        void eventOccurred(AntennaEvent event);
    }
}
