import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

public class MainServer {

        private static int port = 1500;

        public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {

            // on crée le serveur
            Server server = new Server(port);

            // on le démarre
            server.start();

        }
}
