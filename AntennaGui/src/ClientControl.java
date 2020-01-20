import me.alchzh.antenna_control.controller.AntennaController;
import me.alchzh.antenna_control.controller.AntennaScript;
import me.alchzh.antenna_control.controller.AntennaScriptInstruction;
import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.device.AntennaEvent;
import me.alchzh.antenna_control.network.NetworkAntennaDevice;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import static me.alchzh.antenna_control.util.Units.d;

public class ClientControl {
    private JPanel mainPanel;
    private JTextArea controllerTextArea;
    private JButton openScriptButton;
    private JTextField openedScript;
    private JButton runScriptButton;
    private JLabel hostLabel;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton connectButton;
    private JTextField azField;
    private JTextField destAzField;
    private JTextField baseTimeField;
    private JTextField elField;
    private JTextField destElField;
    private JTextField timeField;
    private JButton saveLogAsButton;

    private MeasurementMonitor mm;

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

            controller.addEventListener((AntennaEvent event) -> {
                if (event.type == AntennaEvent.Type.MEASUREMENT) {
                    if (mm == null) {
                        mm = MeasurementMonitor.showMeasurementFrame();
                    }

                    mm.addMeasurement(controller.getAz(), controller.getEl(), event.data);
                } else if (event.type == AntennaEvent.Type.BASE_TIME) {
                    baseTimeField.setText(AntennaController.dtf.format(controller.getBaseTime()));
                }

                updateFields();
            });
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void updateFields() {
        azField.setText(String.format("%.3f", d(controller.getAz())));
        elField.setText(String.format("%.3f", d(controller.getEl())));
        destAzField.setText(String.format("%.3f", d(controller.getDestAz())));
        destElField.setText(String.format("%.3f", d(controller.getDestEl())));
        timeField.setText(String.format("%d", controller.getTime()));
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
        controllerTextArea = new LogArea();

        portSpinner = new JSpinner();
        portSpinner.setValue(52532);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(editor);
    }

    public ClientControl() {
        DefaultCaret caret = (DefaultCaret) controllerTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

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
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        connectButton.addActionListener((event) -> connect());
        saveLogAsButton.addActionListener(e -> saveAs());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Control Panel");
        ClientControl cp = new ClientControl();

        PrintStream ps = new PrintStream(new TextAreaOutputStream(cp.controllerTextArea));

        System.setOut(ps);
        System.setErr(ps);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(cp.mainPanel);
        frame.pack();


        frame.setVisible(true);
    }

    public void saveAs() {
        FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter("Log File", "log");
        final JFileChooser saveAsFileChooser = new JFileChooser();
        saveAsFileChooser.setApproveButtonText("Save");
        saveAsFileChooser.setFileFilter(extensionFilter);
        int actionDialog = saveAsFileChooser.showOpenDialog(null);
        if (actionDialog != JFileChooser.APPROVE_OPTION) {
            return;
        }

        // !! File fileName = new File(SaveAs.getSelectedFile() + ".txt");
        File file = saveAsFileChooser.getSelectedFile();
        if (!file.getName().endsWith(".log")) {
            file = new File(file.getAbsolutePath() + ".log");
        }

        BufferedWriter outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(file));

            controllerTextArea.write(outFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (outFile != null) {
                try {
                    outFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
