package me.alchzh;

public interface AntennaEventListener {
    /**
     * @param eventData Method called when error event received from device
     */
    public void errorEventOccurred(byte[] eventData);

    /**
     * @param eventData Method called when data event received from device
     */
    public void dataEventOccurred(byte[] eventData);
}
