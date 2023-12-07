import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.security.NoSuchAlgorithmException;

public class MainClient {
    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {

        // fenêtre temporaires pour demander l'username

        String userName = JOptionPane.showInputDialog(null, "Enter your username: ", "Username", JOptionPane.PLAIN_MESSAGE);

        // si l'utilisateur annule, ou entre un nom vide on quitte
        if (userName == null || userName.equals("")) {
            System.exit(0);
        }

        // on crée le clientGUI
        new ClientGUI(userName);

    }
}
