import me.alchzh.antenna_control.device.AntennaDevice;
import me.alchzh.antenna_control.mock_device.MockAntennaDevice;
import me.alchzh.antenna_control.network.NetworkAntennaServer;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;

import static me.alchzh.antenna_control.util.Units.u;

public class ServerControl {
    private JTextArea serverTextArea;
    private JButton startServerButton;
    private JButton stopServerButton;
    private JPanel mainPanel;
    private JLabel hostLabel;
    private JTextField hostField;
    private JSpinner portSpinner;

    private AntennaDevice device;
    private NetworkAntennaServer server;
    private Thread listenThread;

    private void startServer() {
        device = new MockAntennaDevice(0, 0, 0, 0, u(135), u(70), u(5 / 1000.0));

        try {
            server = new NetworkAntennaServer(device);

            listenThread = new Thread(() -> {
                try {
                    server.listen(hostField.getText(), (Integer) portSpinner.getValue());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            listenThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerControl() {
        DefaultCaret caret = (DefaultCaret) serverTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        startServerButton.addActionListener(e -> startServer());
        stopServerButton.addActionListener(e -> stopServer());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Server Control");

        ServerControl sc = new ServerControl();
        frame.setContentPane(sc.mainPanel);
        System.setOut(new PrintStream(new TextAreaOutputStream(sc.serverTextArea)));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();


        frame.setVisible(true);
    }

    private void createUIComponents() {
        serverTextArea = new LogArea();

        portSpinner = new JSpinner();
        portSpinner.setValue(52532);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(editor);
    }
}
