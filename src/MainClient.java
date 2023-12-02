import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.security.NoSuchAlgorithmException;

public class MainClient {

    private static int port = 1500;
    private static String server = "localhost";
    private String username;

    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {

        // fenêtre temporaires pour demander l'username

        String userName = JOptionPane.showInputDialog(null, "Enter your username: ", "Username", JOptionPane.PLAIN_MESSAGE);

        // si l'utilisateur annule, on quitte
        if (userName == null) {
            System.exit(0);
        }

        // si l'utilisateur entre un nom vide, on quitte
        if (userName.equals("")) {
            System.exit(0);
        }

        // on crée le clientGUI
        ClientGUI clientGUI = new ClientGUI(port, server, userName);

    }
}
