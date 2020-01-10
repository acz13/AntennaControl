package me.alchzh.antenna_control.device;

@FunctionalInterface
public interface AntennaEventListener {
    /**
     * Called when a regular (data) event is received
     *
     * @param event The event
     */
    void eventOccurred(AntennaEvent event);
}
