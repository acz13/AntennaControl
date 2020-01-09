package me.alchzh.antenna_control;

public interface AntennaDeviceInterface {
    /* COMMAND CODES */
    byte G0 = 0x01; // GOTO AZ EL (DEG DEG)
    byte A0 = 0x02; // DATA ACQUISITION ON OFF (1 OR 0)

    byte PO = 0x08; // POWER ON FROM SLEEP
    byte SD = 0x09; // POWER OFF


    /* RESPONSE EVENT CODES */
    byte CONTROL_SPEED = 0x41; // DEG PER SEC
    byte CONTROL_POSITION_RANGE = 0x42; // MIN_AZ MAX_AZ MIN_EL MAX_EL

    byte COMMAND_ISSUED = 0x50; // CMD <DATA> AZ EL
    byte TRACKING = 0x51; // AZ EL

    byte MEASUREMENT = 0x60; // VALUE

    /* RESPONSE ERROR CODES */
    byte PHYSICAL_POSITION_ERROR = 0x70;
    byte DATA_ACQUISITION_ERROR = 0x71;


    /**
     * @param data Submits a command to antenna device. First byte represents command, arbitrary other bytes
     *             represent data to send to antenna device
     */
    void submitCommand(byte[] data);

    /**
     * @param listener Registers a listener to response data from the device.
     */
    void addListener(AntennaEventListener listener);
}
