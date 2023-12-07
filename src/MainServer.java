import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

public class MainServer {
        public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {

            // on crée le serveur
            Server server = new Server();

            // on le démarre
            server.start();

        }
}
