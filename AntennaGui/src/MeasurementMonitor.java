import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static me.alchzh.antenna_control.util.Hex.bytesToHex;
import static me.alchzh.antenna_control.util.Units.d;

public class MeasurementMonitor {
    private JTextArea measurementTextArea;
    private JPanel mainPanel;
    private JButton saveDataAsButton;
    private ByteBuffer dataBuffer = ByteBuffer.allocate(1024);

    public MeasurementMonitor() {
        DefaultCaret caret = (DefaultCaret) measurementTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        saveDataAsButton.addActionListener(e -> { saveAs(); });
    }

    public void addMeasurement(int az, int el, byte[] data) {
        dataBuffer.clear();
        dataBuffer.put(data);
        dataBuffer.flip();

        int count = dataBuffer.getInt();
        float[] dataValues = new float[count];

        for (int i = 0; i < count; i++) {
            dataValues[i] = dataBuffer.getFloat();
        }

        measurementTextArea.append(String.format("%.3f %.3f : %s \n", d(az), d(el), Arrays.toString(dataValues)));
    }

    private void createUIComponents() {
        measurementTextArea = new LogArea();
    }

    public static MeasurementMonitor showMeasurementFrame() {
        JFrame frame = new JFrame("Measurements");

        MeasurementMonitor mm = new MeasurementMonitor();
        frame.setContentPane(mm.mainPanel);
        frame.pack();

        frame.setVisible(true);

        return mm;
    }

    public void saveAs() {
        FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter("Data File", "dat");
        final JFileChooser saveAsFileChooser = new JFileChooser();
        saveAsFileChooser.setApproveButtonText("Save");
        saveAsFileChooser.setFileFilter(extensionFilter);
        int actionDialog = saveAsFileChooser.showOpenDialog(null);
        if (actionDialog != JFileChooser.APPROVE_OPTION) {
            return;
        }

        // !! File fileName = new File(SaveAs.getSelectedFile() + ".txt");
        File file = saveAsFileChooser.getSelectedFile();
        if (!file.getName().endsWith(".dat")) {
            file = new File(file.getAbsolutePath() + ".dat");
        }

        BufferedWriter outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(file));

            measurementTextArea.write(outFile);
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
