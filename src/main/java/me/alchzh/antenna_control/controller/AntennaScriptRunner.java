package me.alchzh.antenna_control.controller;

import me.alchzh.antenna_control.device.AntennaDevice;

public class AntennaScriptRunner implements Runnable {
    private AntennaScript script;
    private AntennaDevice device;

    public AntennaScriptRunner(AntennaScript script, AntennaDevice device) {
        this.script = script;
        this.device = device;
    }

    @Override
    public void run() {

    }
}