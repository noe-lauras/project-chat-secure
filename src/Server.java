import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;
    private String ip;
    private int port;
    private boolean isRunning;
    private PrintWriter clientWriter;


    public Server(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.isRunning = false;
    }

    public void start() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            this.isRunning = true;
            System.out.println("Server started on port " + this.port);

            while (this.isRunning) {
                Socket clientSocket = serverSocket.accept();
                handleClientConnection(clientSocket);
            }

        } catch (Exception e) {
            System.out.println("Error while starting server: " + e.getMessage());
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            // Example: Read a message from the client
            String receivedMessage = reader.readLine();
            System.out.println("Received from client: " + receivedMessage);

            // si le client envoie "bye", on ferme la connexion
            if (receivedMessage.equals("bye")) {
                this.isRunning = false;
            }

            // Close the connection
            clientSocket.close();

        } catch (Exception e) {
            System.out.println("Error handling client connection: " + e.getMessage());
        }
    }
    public boolean isRunning() {
        return this.isRunning;
    }

    public void sendMessage(String message) {
        if (clientWriter != null) {
            clientWriter.println(message);
        }else{
            System.out.println("No client connected");
        }
    }   
}