import me.alchzh.antenna_control.controller.AntennaController;
import me.alchzh.antenna_control.controller.AntennaScript;
import me.alchzh.antenna_control.controller.AntennaScriptInstruction;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.network.NetworkAntennaDevice;

import javax.swing.*;
import java.io.*;

public class ClientControl {
    private JPanel mainPanel;
    private JTextArea controllerTextArea;
    private JButton openScriptButton;
    private JTextField openedScript;
    private JButton submitButton;
    private JTextField commandField;
    private JButton runScriptButton;
    private JLabel hostLabel;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton connectButton;

    private AntennaDevice device;
    private AntennaController controller;

    private void connect() {
        try {
            portSpinner.commitEdit();
        } catch ( java.text.ParseException e ) { e.printStackTrace(); }

        try {
            System.out.println("Attempting to connect");
            device = new NetworkAntennaDevice(hostField.getText(), (Integer) portSpinner.getValue());
            controller = new AntennaController(device);
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private String chooseFile() {
        JFileChooser chooser = new JFileChooser();

        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        } else {
            return null;
        }
    }

    private void createUIComponents() {
        portSpinner = new JSpinner();
        portSpinner.setValue(52532);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(editor);
    }

    public ClientControl() {
        openScriptButton.addActionListener((event) -> {
            String fileName = chooseFile();
            if (fileName != null) {
                openedScript.setText(fileName);
            }
        });
        runScriptButton.addActionListener((event) -> {
            try {
                BufferedReader in = new BufferedReader(new FileReader(openedScript.getText()));
                AntennaScript script = new AntennaScript(in);

                controller.poweron();
                controller.runScript(script);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        submitButton.addActionListener((event) -> {
            AntennaScriptInstruction instruction = new AntennaScriptInstruction(commandField.getText());
        });
        connectButton.addActionListener((event) -> connect());

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Control Panel");
        ClientControl cp = new ClientControl();

        PrintStream ps = new PrintStream(new TextAreaOutputStream(cp.controllerTextArea));

        System.setOut(ps);
        System.setErr(ps);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(cp.mainPanel);
        frame.pack();


        frame.setVisible(true);
    }
}
