import javax.swing.*;
import java.io.OutputStream;

import static javax.swing.SwingUtilities.invokeLater;

public class TextAreaOutputStream extends OutputStream {

    private final JTextArea textArea;

    public TextAreaOutputStream(final JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public void write(int b) {
        invokeLater(() -> textArea.append(String.valueOf((char)b)));
    }
}