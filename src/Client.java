import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private Socket socket;
    private String ip;
    private int port;
    private boolean isRunning;

    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.isRunning = false;
    }

    public void start() {
        try {
            this.socket = new Socket(this.ip, this.port);
            this.isRunning = true;
            System.out.println("Client started on port " + this.port);

            // Example: Read a message from the server
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String receivedMessage = reader.readLine();
            System.out.println("Received from server: " + receivedMessage);

            // Close the connection
            socket.close();

        } catch (Exception e) {
            System.out.println("Error while starting client: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void sendMessage(String message) {
        try {
            // on crée un socket pour se connecter au serveur
            Socket clientSocket = new Socket(this.ip, this.port);

            // on crée un writer pour envoyer un message au serveur
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // on envoie le message au serveur
            writer.println(message);

            // on crée un reader pour lire la réponse du serveur
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream
                    ()));

            // on lit la réponse du serveur
            String receivedMessage = reader.readLine();

            // on affiche la réponse du serveur
            System.out.println("Received from server: " + receivedMessage);

            // on ferme la connexion
            clientSocket.close();

        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }

    }
}
