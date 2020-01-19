import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static me.alchzh.antenna_control.util.Hex.bytesToHex;
import static me.alchzh.antenna_control.util.Units.d;

public class MeasurementMonitor {
    private JTextArea measurementTextArea;
    private JPanel mainPanel;
    private ByteBuffer dataBuffer = ByteBuffer.allocate(1024);

    public MeasurementMonitor() {
        DefaultCaret caret = (DefaultCaret) measurementTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
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
}
