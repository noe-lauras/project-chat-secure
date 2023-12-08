import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

/*
La classe MainServer est la classe principale du serveur.
Elle lance le serveur.
 */
public class MainServer {
        public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {

            // on crée le serveur
            Server server = new Server(1500);

            // on le démarre
            server.start();

        }
}
