import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;


/*
La classe Client est le client du chat. Il se connecte au serveur et envoie des messages.
Il a besoin d'un thread pour écouter le serveur en permanence.
attributs:
	- sInput: pour lire les messages du serveur
	- sOutput: pour écrire sur le socket
	- socket: le socket pour se connecter au serveur
	- server: le serveur auquel on se connecte
	- username: le nom d'utilisateur
	- port: le port de connexion
	- messageListener: le callback pour informer l'interface graphique
	- listenThread: le thread pour écouter le serveur
	- dhKey: la clé de Diffie-Hellman
	- serverPublicKey: la clé publique du serveur
méthodes:
	- Client: constructeur
	- start: pour démarrer le chat
	- display: pour afficher un message dans la console
	- sendMessage: pour envoyer un message au serveur
	- disconnect: pour se déconnecter du serveur
	- setMessageListener: pour définir le callback
	- ListenFromServer: sous classe pour écouter le serveur

 */
public class Client  {
	private  ListenFromServer listenThread;
	private MessageListener messageListener;
	private static final int DEFAULT_PORT = 1500;
	private ObjectInputStream sInput;
	private ObjectOutputStream sOutput;
	private Socket socket;
	private String serverAddress="";
	private final String username;
	private DHKey dhKey;
	private PublicKey serverPublicKey;

	/*
	 * Constructeur:
	 * Il prend en paramètre le nom d'utilisateur et l'adresse du serveur
	 * On initialise les attributs
	 * On génère la clé publique et privée de Diffie-Hellman : dhKey.generateKeys()
	 *
	 */
	Client(String username) throws NoSuchPaddingException, NoSuchAlgorithmException {
		this.username = username;
		this.dhKey = new DHKey();
		dhKey.generateKeys();
		//La méthode ping gère l'ip donc on n'a pas besoin de la préciser
        //Le port est toujours 1500
        String resPing=ping();
        if(resPing.equals("")){
            System.out.println("Le serveur n'a pas pu être ping !");
            this.serverAddress="localhost";
            //TODO mettre le pop up pour rentrer l'ip manuellement
        }
        else{
            this.serverAddress=resPing;
        }
    }

	public static String ping(){
        int receivePort = DEFAULT_PORT+1 ; // Port pour recevoir les réponses
        String adresseServeur="";
        // Message à envoyer
        String message = "Serveur je te parle";

        DatagramSocket socketReception;
        try {
            socketReception = new DatagramSocket(receivePort);
            // Défini un timeout de 5 secondes pour sortir si on attend trop
            socketReception.setSoTimeout(5000);
            DatagramSocket socketEnvoi = new DatagramSocket();
            byte[] messageBytes = message.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, broadcastAddress, DEFAULT_PORT);
            socketEnvoi.send(packet);
            socketEnvoi.close();
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
            socketReception.receive(receivePacket);

            InetAddress clientAddress = receivePacket.getAddress();
            String message2 = new String(receivePacket.getData(), 0, receivePacket.getLength());
            if(message2.equals("Client je te réponds")){
                System.out.println(message2);
                adresseServeur=clientAddress.getHostAddress();
                System.out.println(adresseServeur);
                socketReception.close();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Erreur: Le délai d'attente pour la réponse est dépassé.");
        } catch (IOException ignored) {}
        return adresseServeur;
    }

	/*
	 * start: pour démarrer le chat
	 * on tente d'instancier un socket avec l'adresse du serveur et le port par défaut
	 * si ça échoue, on affiche une erreur et on retourne false -> le main sait que la connexion a échoué
	 * sinon, on affiche un message de connexion réussie
	 * on crée les flux d'entrée et de sortie :
	 * 		- sInput pour lire les messages du serveur
	 * 		- sOutput pour écrire sur le socket
	 * on crée le thread pour écouter le serveur : listenThread = new ListenFromServer();
	 * on démarre le thread : listenThread.start();
	 * on envoie le nom d'utilisateur au serveur en tant que String. Puis on envoie notre clé publique au serveur
	 * 		- sOutput.writeObject(username);
	 * 		- sOutput.writeObject(dhKey.getPublicKey());
	 * si ça échoue, on affiche une erreur et on retourne false -> le main sait que l'envoi a échoué
	 * sinon, on retourne true -> le main sait que la connexion a réussi
	 */
	public boolean start() {

		try { socket = new Socket(serverAddress,DEFAULT_PORT);}
		catch(Exception ec) { display("Error connectiong to server:" + ec);
			return false; }
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
		try { sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());}
		catch (IOException eIO) { display("Exception creating new Input/output Streams: " + eIO);
			return false;}
		listenThread = new ListenFromServer();
		listenThread.start();
		try
		{   sOutput.writeObject(username);
			sOutput.writeObject(dhKey.getPublicKey());
		}
		catch (IOException eIO) { display("Exception doing login : " + eIO);
			disconnect();
			return false; }
		return true;
	}

	/*
	 * Pour afficher un message dans la console, avec un retour à la ligne
	 */
	private void display(String msg) {
		System.out.println(msg);
	}

	/*
	 * sendMessage: pour envoyer un message au serveur
	 * on crypte le message avec la méthode encrypt de la classe DHKey
	 * on envoie le message crypté au serveur
	 * si ça échoue, on affiche une erreur
	 */
	void sendMessage(Message msg) {
		try {
			msg.setMessage(dhKey.encrypt(msg));
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * disconnect: pour se déconnecter du serveur
	 * on ferme les flux d'entrée et de sortie, et le socket
	 * on interrompt le thread pour écouter le serveur
	 * on ignore les erreurs car on ne peut rien faire si ça échoue
	 */
	void disconnect() {
		try {
			if(sInput != null) sInput.close();
			if(sOutput != null) sOutput.close();
			if(socket != null) socket.close();
			if(listenThread != null) listenThread.interrupt();
		}
		catch(Exception ignored) {}
	}

	/*
	 * setMessageListener: pour définir le callback
	 * le callback est une interface MessageListener qui a une méthode onMessageReceived
	 * on définit le callback avec la méthode setMessageListener, comme ça, on peut appeler la méthode onMessageReceived
	 */
	public void setMessageListener(MessageListener listener) {
		this.messageListener = listener;
	}

	/*
	 * ListenFromServer: sous classe pour écouter le serveur
	 * on écoute en permanence le serveur (tant que le thread n'est pas interrompu)
	 * si on reçoit un message :
	 * 		- si c'est un String : on appelle simplement la méthode onMessageReceived du callback, qui va actualiser l'interface graphique
	 * 		- si c'est une clé publique : c'est la clé publique du serveur, on la stocke dans la DhKey du client pour pouvoir crypter les messages
	 * si on reçoit une erreur, on affiche un message d'erreur et on arrête le thread
	 *
	 */
	class ListenFromServer extends Thread {
		public void run() {
			while (true) {
				try {
					Object recu = sInput.readObject();
					if (recu instanceof String message) {
						System.out.println(message);
						if (messageListener != null) {
							messageListener.onMessageReceived(message);
						}
					} else if (recu instanceof PublicKey) {
						serverPublicKey = (PublicKey) recu;
						dhKey.setReceivedPublicKey(serverPublicKey);
						dhKey.generateCommonSecretKey();
					}
					System.out.print("> ");
				} catch (IOException e) {
					// notification
					String notif = " *** ";
					display(notif + "Server has closed the connection: " + e + notif);
					break;
				} catch (ClassNotFoundException ignored) {
				}
			}
		}
	}
}