package me.alchzh.antenna_control.device;

public interface AntennaDevice {
    /**
     * @param listener Registers a listener to response data from the device.
     */
    void addEventListener(AntennaDevice.Listener listener);

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
     * @param listener Unregisters a listener to response data from the device.
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
