package me.alchzh.antenna_control;

public interface AntennaEventListener {
    /**
     * @param eventData Method called when error event received from device
     */
    void errorEventOccurred(byte[] eventData);

    /**
     * @param eventData Method called when data event received from device
     */
    void dataEventOccurred(byte[] eventData);
}
