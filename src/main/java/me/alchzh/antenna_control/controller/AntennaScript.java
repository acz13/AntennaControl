package me.alchzh.antenna_control.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import me.alchzh.antenna_control.device.AntennaDevice;

public class AntennaScript {
    private ArrayList<AntennaScriptInstruction> instructions = new ArrayList<>();
    private Map<String, Integer> labels = new HashMap<>();

    static class AntennaScriptInstruction {
        public static final Pattern pattern = Pattern.compile("\\$([A-Z_0-9]+)");

        public final String command;
        public final List<String> arguments;

        public AntennaScriptInstruction(String line) {
            Matcher m = pattern.matcher(line);

            m.find();
            command = m.group(1);

            ArrayList<String> tmpArguments = new ArrayList<String>();
            while (m.find()) {
                tmpArguments.add(m.group(1).toUpperCase());
            }
            arguments = Collections.unmodifiableList(tmpArguments);
        }
    }

    class AntennaScriptRunner implements Runnable {
        private int scCursor = 0;
        private AntennaDevice device;

        public AntennaScriptRunner(AntennaDevice device) {
            this.device = device;
        }

        @Override
        public void run() {

        }
    }

    public AntennaScriptRunner attach(AntennaDevice device) {
        return new AntennaScriptRunner(device);
    }
}