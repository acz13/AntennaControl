package me.alchzh.antenna_control.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single instruction in a script. Each instruction is in the form of a command and a list of arguments.
 */
public class AntennaScriptInstruction {
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
