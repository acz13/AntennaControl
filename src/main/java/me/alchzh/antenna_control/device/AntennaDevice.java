package me.alchzh.antenna_control.device;

/**
 * Interface representing an AntennaDevice
 * Essentially functions as a two way event bus with listeners
 */
public interface AntennaDevice extends EventEmitter<AntennaEvent> {
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
}
