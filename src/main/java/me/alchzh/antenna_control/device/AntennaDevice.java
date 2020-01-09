package me.alchzh.antenna_control.device;

public interface AntennaDevice {
    /* COMMAND CODES */
    byte G0 = 0x01; // GOTO AZ EL (DEG DEG)
    byte A0 = 0x02; // DATA ACQUISITION ON OFF (1 OR 0)

    byte POWERON = 0x08; // POWER ON FROM SLEEP
    byte POWEROFF = 0x09; // POWER OFF


    /* RESPONSE EVENT CODES */
    byte POSITION_UNIT_SIZE = 0x40; // SIZE (BYTES) OF POSITION
    byte CONTROL_SPEED = 0x41; // UNIT PER SEC
    byte CONTROL_POSITION_RANGE = 0x42; // MIN_AZ MAX_AZ MIN_EL MAX_EL
    byte CONTROL_BASE_POSITION = 0x43; // AZ EL
    byte BASE_TIME = 0x44; // 8 BYTE UNIX TIME (LONG)

    byte COMMAND_ISSUED = 0x50; // CMD <DATA>
    byte CURRENT_STATE = 0x51; // AZ EL DEST_AZ DEST_EL

    byte MEASUREMENT = 0x60; // VALUE

    /* RESPONSE ERROR CODES */
    byte PHYSICAL_POSITION_ERROR = 0x70;
    byte DATA_ACQUISITION_ERROR = 0x71;
    byte UNKNOWN_COMMMAND_ERROR = 0x72;


    /**
     * @param data Submits a command to antenna device. First byte represents command, arbitrary other bytes
     *             represent data to send to antenna device
     */
    void submitCommand(byte[] data);

    /**
     * @param listener Registers a listener to response data from the device.
     */
    void addEventListener(AntennaEventListener listener);

    /**
     * @param listener Unregisters a listener to response data from the device.
     */
    void removeEventListener(AntennaEventListener listener);
}
