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
     * @param code Command code
     * @param data Command data / arguments
     */
    default void submitCommand(byte code, byte... data) {
        submitCommand(new AntennaCommand(code, data));
    }

    /**
     * Submits a command to the device
     *
     * @param code Command code
     * @param data Command data / arguments
     */
    default void submitCommand(byte code, int... data) {
        submitCommand(new AntennaCommand(code, data));
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
