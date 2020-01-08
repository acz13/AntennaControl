package me.alchzh;

import java.util.ArrayList;

public class MockAntennaDevice implements AntennaDeviceInterface {
    private ArrayList<AntennaEventListener> listeners = new ArrayList<AntennaEventListener>();

    @Override
    public void submitCommand(byte[] data) {

    }

    /**
     * @param data Sends an error event to every registered listener
     */
    private void sendError(byte[] data) {
        for (AntennaEventListener listener : listeners) {
            listener.errorEventOccurred(data);
        }
    }

    /**
     * @param data Sends a data event to every registered listener
     */
    private void sendData(byte[] data) {
        for (AntennaEventListener listener : listeners) {
            listener.dataEventOccurred(data);
        }
    }

    @Override
    public void addListener(AntennaEventListener listener) {
        listeners.add(listener);
    }
}
