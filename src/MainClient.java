import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.security.NoSuchAlgorithmException;


/*
La classe MainClient est la classe principale du client.
Elle demande le nom d'utilisateur et lance le clientGUI : la fenêtre de chat.
 */
public class MainClient {
    private static final String SERVER_ADDRESS = "localhost";
    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {

        String userName = JOptionPane.showInputDialog(null, "Enter your username: ", "Username", JOptionPane.PLAIN_MESSAGE);

        // si l'utilisateur annule, ou entre un nom vide, on quitte
        if (userName == null || userName.equals("")) {
            System.exit(0);
        }

        // on crée le clientGUI
        new ClientGUI(userName, SERVER_ADDRESS);

    }
}
