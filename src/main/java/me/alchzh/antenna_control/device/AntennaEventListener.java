package me.alchzh.antenna_control.device;

public interface AntennaEventListener {
    /**
     * @param eventData Method called when error event received from device
     */
    void errorEventOccurred(AntennaEvent event);

    /**
     * @param eventData Method called when data event received from device
     */
    void dataEventOccurred(AntennaEvent event);
}
