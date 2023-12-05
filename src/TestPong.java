import java.io.IOException;
import java.net.*;

public class TestPong {
    public static void main(String[] args) {
        int receivePort = 1500; // Port pour recevoir les messages
        int sendPort = 1501; // Port pour envoyer les réponses

        try {
            DatagramSocket socket = new DatagramSocket(receivePort);
            //System.out.println("En attente de messages...");
            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                InetAddress clientAddress = receivePacket.getAddress();
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                if (message.equals("Serveur je te parle")) {
                    DatagramSocket responseSocket = new DatagramSocket();
                    String responseMessage = "Client je te réponds";
                    byte[] sendData = responseMessage.getBytes();

                    InetAddress destinationAddress = InetAddress.getByName(clientAddress.getHostAddress());
                    DatagramPacket responsePacket = new DatagramPacket(sendData, sendData.length, destinationAddress, sendPort);
                    responseSocket.send(responsePacket);
                    responseSocket.close();
                    //System.out.println("Message envoyé avec succès !");
                }
            }
            // Ajouter la fermeture de la socket ici si nécessaire, par exemple sur un signal de sortie.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
