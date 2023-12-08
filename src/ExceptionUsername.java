import javax.swing.*;

public class ExceptionUsername extends JFrame {
    public ExceptionUsername(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(null, message, "Erreur", JOptionPane.ERROR_MESSAGE);

    }
}
