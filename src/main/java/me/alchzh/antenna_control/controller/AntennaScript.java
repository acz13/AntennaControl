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

import static me.alchzh.antenna_control.util.Units.d;
import static me.alchzh.antenna_control.util.Units.u;

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
    private static final Pattern unitsPattern = Pattern.compile("^(\\d+)u$");
    private static final Pattern degreesPattern = Pattern.compile("^(\\d+(\\.\\d*)?)d?$");

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


    /** Creates a new runner from this script attached to the device
     * @param controller Device to attach to
     * @return Runner (callable) that runs the script on this device in a new thread.
     */
    public AntennaScriptRunner attach(AntennaController controller) {
        return new AntennaScriptRunner(controller);
    }

    /**
     * AntennaScriptRunner attaches to a device and is the Callable that the script runs as in a thread
     */
    private class AntennaScriptRunner implements Callable<Void> {
        /**
         * scCursor represents the current index in the instructions to execute
         */
        private int scCursor = 0;
        private final AntennaController controller;
        private final AntennaDevice device;
        private final Map<String, Integer> variables = new HashMap<>();
        private final Map<String, Double> positions = new HashMap<>();


        /**
         * Create AntennaScriptRunner to run on specified device
         *
         * @param controller Device to run commands on
         */
        public AntennaScriptRunner(AntennaController controller) {
            this.controller = controller;
            this.device = controller.getDevice();
        }


        /**
         * Get the position degrees presented by a specific argument
         * TODO: move this to the parser somehow
         *
         * @param argument The argument
         * @return Position (degrees) it represents
         */
        private double getPosition(String argument) {
            if (argument == null) {
                throw new IllegalArgumentException("Something went very, very wrong");
            }

            switch (argument) {
                case "AZ":
                    return d(controller.getAz());
                case "El":
                    return d(controller.getEl());
                case "BASE_AZ":
                    return d(controller.getBaseAz());
                case "BASE_EL":
                    return d(controller.getBaseEl());
            }

            Matcher unitMatcher;
            if ((unitMatcher = unitsPattern.matcher(argument)).matches()) {
                return d(Integer.parseInt(unitMatcher.group(1)));
            }

            Matcher degreesMatcher;
            if ((degreesMatcher = degreesPattern.matcher(argument)).matches()) {
                return Double.parseDouble(degreesMatcher.group(1));
            }

            Double fromVar;
            if ((fromVar = positions.get(argument)) != null) {
                return fromVar;
            }

            throw new IllegalArgumentException("Not a position " + argument);
        }

        /**
         * Get the integer presented by a specific argument
         * TODO: move this to the parser somehow
         *
         * @param argument The argument
         * @return Integer it represents
         */
        public int getInteger(String argument) {
            if (argument == null) {
                throw new IllegalArgumentException("Something went very, very wrong");
            }

            switch (argument) {
                case "TIME":
                    return controller.getTime();
                case "LAST_TIME":
                    return controller.getLastEventTime();
            }

            try {
                return Integer.parseInt(argument);
            } catch (NumberFormatException e) {
                Integer fromVar;
                if ((fromVar = variables.get(argument)) != null) {
                    return fromVar;
                }
            }

            throw new IllegalArgumentException("Not an integer " + argument);
        }

        @Override
        public Void call() throws InterruptedException {
            // while we haven't reached the end of the script
            while (scCursor < instructions.size()) {
                AntennaScriptInstruction instr = instructions.get(scCursor);

                System.out.println(instr);

                List<String> args = instr.arguments;
                switch (instr.command) {
                    // DEVICE COMMANDS
                    case "G0": {
                        double AZ = getPosition(args.get(0));
                        double EL = getPosition(args.get(1));

                        // Unfreezes the script when a MOVE_FINISHED event is received
                        AntennaDevice.Listener notifier = (AntennaEvent event) -> {
                            synchronized (instr) {
                                if (event.type == AntennaEvent.Type.MOVE_FINISHED
                                        || event.type == AntennaEvent.Type.MOVE_CANCELED) {
                                    instr.notify();
                                }
                            }
                        };

                        device.addEventListener(notifier);
                        device.submitCommand(AntennaCommand.Type.G0, u(AZ), u(EL));

                        // Freezes the script runner until a MOVE_FINISHED event is received.
                        synchronized (instr) {
                            instr.wait();

                            device.removeEventListener(notifier);
                        }

                        break;
                    }
                    case "T0":
                        device.submitCommand(AntennaCommand.Type.G0, (byte) getInteger(args.get(0)));
                        break;

                    // SCRIPTING LANGUAGE FEATURES
                    case "LABEL":
                    case "":
                    case "#":
                        break;
                    case "GOTO": {
                        String LABEL = args.get(0);
                        scCursor = labels.get(LABEL);
                        continue;
                    }
                    case "GOTOIF": {
                        if (getInteger(args.get(0)) > 0) {
                            String LABEL = args.get(1);
                            scCursor = labels.get(LABEL);
                            continue;
                        }
                        break;
                    }
                    case "EXIT":
                        return null;
                    case "VAR":
                    case "INT":
                    case "SETVAR":
                    case "SETINT":
                    case "DEFVAR":
                    case "DEFINT": {
                        String varName = args.get(0);
                        if (positions.containsKey(varName)) {
                            throw new IllegalArgumentException(varName + " is already defined as a position.");
                        }

                        int varValue = args.size() > 1 ? getInteger(args.get(1)) : 0;

                        variables.put(varName, varValue);
                        break;
                    }
                    case "POS":
                    case "SETP":
                    case "DEFP": {
                        String posName = args.get(0);
                        if (variables.containsKey(posName)) {
                            throw new IllegalArgumentException(posName + " is already defined as an integer var.");
                        }

                        double posValue = args.size() > 1 ? getPosition(args.get(1)) : 0;

                        positions.put(posName, posValue);
                        break;
                    }
                    case "INC":
                    case "ADD": {
                        String varName = args.get(0);
                        variables.put(varName, variables.get(varName) +
                                (args.size() > 1 ? getInteger(args.get(1)) : 1));
                        break;
                    }
                    case "INCP":
                    case "ADDP": {
                        String posName = args.get(0);
                        positions.put(posName, positions.get(posName) +
                                (args.size() > 1 ? getInteger(args.get(1)) : 1));
                        break;
                    }
                    case "DEC":
                    case "SUB": {
                        String varName = args.get(0);
                        variables.put(varName, variables.get(varName) -
                                (args.size() > 1 ? getInteger(args.get(1)) : 1));
                        break;
                    }
                    case "DECP":
                    case "SUBP": {
                        String posName = args.get(0);
                        positions.put(posName, positions.get(posName) -
                                (args.size() > 1 ? getInteger(args.get(1)) : 1));
                        break;
                    }
                    case "MUL": {
                        String varName = args.get(0);
                        variables.put(varName, variables.get(varName) * getInteger(args.get(1)));
                        break;
                    }
                    case "MULP": {
                        String posName = args.get(0);
                        positions.put(posName, positions.get(posName) * getPosition(args.get(1)));
                        break;
                    }
                    case "DIV": {
                        String varName = args.get(0);
                        variables.put(varName, variables.get(varName) / getInteger(args.get(1)));
                        break;
                    }
                    case "DIVP": {
                        String posName = args.get(0);
                        positions.put(posName, positions.get(posName) / getPosition(args.get(1)));
                        break;
                    }
                    case "DIVPI": {
                        String posName = args.get(0);
                        positions.put(posName, positions.get(posName) / getInteger(args.get(1)));
                        break;
                    }
                    case "LESS": {
                        String output = args.size() > 2 ? args.get(2) : "_";
                        variables.put(output, getInteger(args.get(0)) < getInteger(args.get(1)) ? 1 : 0);
                        break;
                    }
                    case "GREATER": {
                        String output = args.size() > 2 ? args.get(2) : "_";
                        variables.put(output, getInteger(args.get(0)) > getInteger(args.get(1)) ? 1 : 0);
                        break;
                    }
                    case "EQUAL": {
                        String output = args.size() > 2 ? args.get(2) : "_";
                        variables.put(output, getInteger(args.get(0)) == getInteger(args.get(1)) ? 1 : 0);
                        break;
                    }
                    case "WAIT": {
                        TimeUnit.MILLISECONDS.sleep(getInteger(args.get(0)));
                        break;
                    }
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