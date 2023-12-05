import java.io.IOException;
import java.net.*;

public class TestPing {
    public static String ping(){
                int sendPort = 1500; // Port pour envoyer le message
        int receivePort = 1501; // Port pour recevoir les réponses
        String adresseServeur="";
        // Message à envoyer
        String message = "Serveur je te parle";

        DatagramSocket socketReception;
        try {
            socketReception = new DatagramSocket(receivePort);
            //System.out.println("En attente de messages...");

            DatagramSocket socketEnvoi = new DatagramSocket();
            byte[] messageBytes = message.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, broadcastAddress, sendPort);
            socketEnvoi.send(packet);
            socketEnvoi.close();
            //System.out.println("Message envoyé avec succès !");

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socketReception.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                String message2 = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if(message2.equals("Client je te réponds")){
                     System.out.println(message2);
                     adresseServeur=clientAddress.getHostAddress();
                    System.out.println(adresseServeur);
                    socketReception.close();
                }
            }
        } catch (IOException ignored) {}
        return adresseServeur;
    }
    public static void main(String[] args) {
        System.out.println(ping());
    }
}
