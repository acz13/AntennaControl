package me.alchzh.antenna_control.controller;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AntennaScript {
    private final ArrayList<AntennaScriptInstruction> instructions = new ArrayList<>();
    private final Map<String, Integer> labels = new HashMap<>();

    public AntennaScript(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#")) continue;
            if (line.startsWith("ENDSCRIPT")) break;

            AntennaScriptInstruction instruction = new AntennaScriptInstruction(line);

            if (instruction.command == null) {
                continue;
            } else if (instruction.command.equals("LABEL")) {
                labels.put(instruction.arguments.get(0), instructions.size());
            }

            instructions.add(instruction);
        }
    }

    public static int d(double degrees) {
        return (int) (degrees * (Integer.MAX_VALUE / 180.0));
    }

    @Override
    public String toString() {
        return instructions.stream()
                .map(AntennaScriptInstruction::toString)
                .collect(Collectors.joining(" "));
    }

    static class AntennaScriptInstruction {
        public static final Pattern pattern = Pattern.compile("([#A-Z_0-9]+)");

        public final String command;
        public final List<String> arguments;

        public AntennaScriptInstruction(String line) {
            Matcher m = pattern.matcher(line);

            if (!m.find()) {
                command = "";
                arguments = Collections.emptyList();
                return;
            }

            command = m.group(1);

            ArrayList<String> tmpArguments = new ArrayList<>();
            while (m.find()) {
                tmpArguments.add(m.group(1).toUpperCase());
            }
            arguments = Collections.unmodifiableList(tmpArguments);
        }

        @Override
        public String toString() {
            return command + " " + String.join(" ", arguments);
        }
    }

    public AntennaScriptRunner attach(AntennaDevice device) {
        return new AntennaScriptRunner(device);
    }

    class AntennaScriptRunner implements Callable<Void> {
        private int scCursor = 0;
        private final AntennaDevice device;

        public AntennaScriptRunner(AntennaDevice device) {
            this.device = device;
        }

        @Override
        public Void call() throws InterruptedException {
            while (scCursor < instructions.size()) {
                AntennaScriptInstruction instr = instructions.get(scCursor);

                System.out.println(instr);

                switch (instr.command) {
                    case "LABEL":
                    case "":
                    case "#":
                        break;
                    case "GOTO":
                        String label = instr.arguments.get(0);
                        scCursor = labels.get(label);
                        continue;
                    case "EXIT":
                        return null;
                    case "G0":
                        int[] intArr = instr.arguments.stream()
                                .mapToInt(Integer::parseInt)
                                .map(AntennaScript::d)
                                .toArray();

                        AntennaDevice.Listener notifier = (AntennaEvent event) -> {
                            synchronized (instr) {
                                if (event.type == AntennaEvent.Type.MOVE_FINISHED) {
                                    System.out.println("unlocking");
                                    instr.notify();
                                }
                            }
                        };

                        device.addEventListener(notifier);
                        device.submitCommand(AntennaCommand.Type.G0, intArr);

                        synchronized (instr) {
                            instr.wait();

                            device.removeEventListener(notifier);
                        }

                        break;
                    case "T0":
                        device.submitCommand(AntennaCommand.Type.G0, Byte.parseByte(instr.arguments.get(0)));
                        break;
                    case "WAIT":
                        TimeUnit.MILLISECONDS.sleep(Long.parseLong(instr.arguments.get(0)));
                        break;
                    default:
                        throw new UnsupportedOperationException(instr.command);
                }

                scCursor++;
            }

            return null;
        }
    }
}