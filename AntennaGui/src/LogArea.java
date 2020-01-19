import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

public class LogArea extends JTextArea {
    public static final Font monospaced = new Font("Monospaced", Font.PLAIN, 12);

    public LogArea() {
        super();

        setFont(monospaced);
    }
}
