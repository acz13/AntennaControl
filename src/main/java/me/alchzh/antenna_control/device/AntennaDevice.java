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
     * @param listener Registers a listener to response data from the device.
     */
    void addEventListener(AntennaEventListener listener);

    /**
     * @param listener Unregisters a listener to response data from the device.
     */
    void removeEventListener(AntennaEventListener listener);
}
