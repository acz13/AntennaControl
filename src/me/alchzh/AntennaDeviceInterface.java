package me.alchzh;

public interface AntennaDeviceInterface {
    /* COMMAND CODES */
    public static byte G0 = 0x01; // GOTO AZ EL (DEG DEG)
    public static byte A0 = 0x02; // DATA ACQUISITION ON OFF (1 OR 0)

    public static byte PO = 0x08; // POWER ON FROM SLEEP
    public static byte SD = 0x09; // POWER OFF


    /* RESPONSE EVENT CODES */
    public static byte CONTROL_SPEED = 0x41; // DEG PER SEC
    public static byte CONTROL_POSITION_RANGE = 0x42; // MIN_AZ MAX_AZ MIN_EL MAX_EL

    public static byte COMMAND_ISSUED = 0x50; // CMD <DATA> AZ EL
    public static byte TRACKING = 0x51; // AZ EL

    public static byte MEASUREMENT = 0x60; // VALUE

    /* RESPONSE ERROR CODES */
    public static byte PHYSICAL_POSITION_ERROR = 0x70;
    public static byte DATA_ACQUISITION_ERROR = 0x71;


    /**
     * @param data Submits a command to antenna device. First byte represents command, arbitrary other bytes
     *             represent data to send to antenna device
     */
    public void submitCommand(byte[] data);

    /**
     * @param listener Registers a listener to response data from the device.
     */
    public void addListener(AntennaEventListener listener);
}
