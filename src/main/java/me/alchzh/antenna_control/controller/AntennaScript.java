package me.alchzh.antenna_control.controller;

import me.alchzh.antenna_control.device.AntennaCommand;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaEvent;
import me.alchzh.antenna_control.util.Units;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An abstract, runnable representation of a script
 * <p>
 * Comments should start with #
 * The following commands are currently implemented:
 * <ul>
 *     <li>LABEL [NAME]</li>
 *     <li>GOTO [LABEL]</li>
 *     <li>G0 [AZ] [EL] (blocks until move completed)</li>
 *     <li>WAIT [MILLI}</li>
 *     <li>EXIT</li>
 * </ul>
 */
public class AntennaScript {
    /**
     * The list of instructions represents the entire script (no code blocks).
     */
    private final List<AntennaScriptInstruction> instructions = new ArrayList<>();
    /**
     * Label locations for GOTO statement in terms of indices in instructions list.
     */
    private final Map<String, Integer> labels = new HashMap<>();

    /**
     * Read a script from a BufferedReader, one instruction per line, usually from a file.
     * If from standard input, use ENDSCRIPT to end script
     *
     * @param br Buffered reader to read from
     * @throws IOException On any IOException
     */
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

    @Override
    public String toString() {
        return instructions.stream()
                .map(AntennaScriptInstruction::toString)
                .collect(Collectors.joining(" "));
    }

    /**
     * A single instruction in a script. Each instruction is in the form of a command and a list of arguments.
     */
    static class AntennaScriptInstruction {
        /**
         * Pattern that recognizes every valid token in a line
         */
        public static final Pattern pattern = Pattern.compile("([#A-Z_0-9.]+)");

        /**
         * Specifies the action of the instruction.
         */
        public final String command;
        /**
         * Additional arguments to be passed
         */
        public final List<String> arguments;

        /**
         * Parses an instruction from valid tokens in a line
         *
         * @param line Line of script file to parse
         */
        public AntennaScriptInstruction(String line) {
            // Match valid tokens
            Matcher m = pattern.matcher(line);

            // Finds the first valid token. Sets that as the command.
            if (!m.find()) {
                // Empty instruction (blank or no valid tokens)
                command = "";
                arguments = Collections.emptyList();
                return;
            }

            // m.group(1) returns our entire first match as a string since the capture group is the entire pattern.
            command = m.group(1);

            // Add arguments into temporary list and freeze it afterward
            ArrayList<String> tmpArguments = new ArrayList<>();
            while (m.find()) {
                tmpArguments.add(m.group(1).toUpperCase());
            }

            // Prevent list from being modified
            arguments = Collections.unmodifiableList(tmpArguments);
        }

        @Override
        public String toString() {
            return command + " " + String.join(" ", arguments);
        }
    }


    /**
     * @param device Device to attach to
     * @return Creates a new runner from this script attached to the device.
     */
    public AntennaScriptRunner attach(AntennaDevice device) {
        return new AntennaScriptRunner(device);
    }

    /**
     * AntennaScriptRunner attaches to a device and is the Callable that the script runs as in a thread
     */
    class AntennaScriptRunner implements Callable<Void> {
        /**
         * scCursor represents the current index in the instructions to execute
         */
        private int scCursor = 0;
        private final AntennaDevice device;

        /**
         * Create AntennaScriptRunner to run on specified device
         *
         * @param device Device to run commands on
         */
        public AntennaScriptRunner(AntennaDevice device) {
            this.device = device;
        }

        @Override
        public Void call() throws InterruptedException {
            // while we haven't reached the end of the script
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
                        // Maps strings to integers
                        int[] intArr = instr.arguments.stream()
                                .mapToInt(Integer::parseInt)
                                .map(Units::u)
                                .toArray();

                        // Unfreezes the script when a MOVE_FINISHED event is received
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

                        // Freezes the script runner until a MOVE_FINISHED event is received.
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

            // Make sure we return something for the Callable
            return null;
        }
    }
}