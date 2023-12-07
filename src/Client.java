import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;


// La classe Client qui peut être exécutée en mode console
public class Client  {
	private  ListenFromServer listenThread; // pour ecouter le serveur
	private MessageListener messageListener; // pour afficher les messages
	private static final int DEFAULT_PORT = 1500;
	private ObjectInputStream sInput;// pour lire le socket
	private ObjectOutputStream sOutput;// pour écrire sur le socket
	private Socket socket;// socket object
	private String serverAddress="";// Adresse du serveur
	private final String username;// username
	//TODO Enlever tout ce qui concerne l'AES
	private final AES aes; // clé de cryptage AES
	private final DHKey dhKey;
	/*
	 * Constructeur appelé par la console
	 * username: le nom d'utilisateur
	 */

	Client(String username) throws NoSuchPaddingException, NoSuchAlgorithmException {
		this.username = username;
		this.aes=new AES();
		this.dhKey=new DHKey();
		//La méthode ping gère l'ip donc on n'a pas besoin de la préciser
		//Le port est toujours 1500
		String resPing=ping();
		if(resPing.equals("")){
			System.out.println("Pour l'instant ya r");
			//TODO mettre le pop up pour rentrer l'ip manuellement
		}
		else{
			this.serverAddress=resPing;
		}
	}
	//Fonction qui permet de récupérer directement l'IP du serveur sans la taper en dur
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
		} catch (SocketTimeoutException e) {
			System.out.println("Erreur: Le délai d'attente pour la réponse est dépassé.");
		} catch (IOException ignored) {}
		return adresseServeur;
	}

	//Pour démarrer le chat
	public boolean start() {
		// essaie de se connecter au serveur
		try {
			socket = new Socket(serverAddress,DEFAULT_PORT);
		}
		// exception si echec
		catch(Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}
		//Sinon celle-ci est acceptée
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);

		/*
		 * Creation des flux d'entrée et de sortie
		 */

		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());

		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// création du thread pour écouter le serveur, on le stocke pour pouvoir l'arrêter plus tard

		listenThread = new ListenFromServer();
		listenThread.start();
		// Envoi du nom d'utilisateur au serveur en tant que String. Tous les autres messages seront des objets ChatMessage et non des Strings.
		try
		{
			sOutput.writeObject(username);
		}
		catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// tout est ok, on retourne true, pour informer le main que la connexion est ok
		return true;
	}

	// afficher un message dans la console
	private void display(String msg) {
		System.out.println(msg);
	}

	/*
	 * Pour envoyer un message au serveur
	 */
	void sendMessage(Message msg) {
		try {
			System.out.println(msg.getMessage());
			// on encrypte le message
			msg.setMessage((aes.encrypt((String) msg.getMessage())));
			System.out.println(Arrays.toString((byte[])msg.getMessage()));
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * Si le client se deconnecte, on ferme les flux et le socket
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

	public void setMessageListener(MessageListener listener) {
		this.messageListener = listener;
	}

	/*
	 * sous classe pour ecouter le serveur, en tant que thread, pour ne pas bloquer le client avec la lecture du socket
	 */
	class ListenFromServer extends Thread {
		public void run() {
			while (true) {
				try {
					// lecture du message du serveur provenant du socket (sInput)
					Object recu = sInput.readObject();
					// Là, je teste si recu est un String (un message normal) ou un Byte[] (la clé)
					if (recu instanceof String message) {
						System.out.println(message);

						// Appeler le callback pour informer l'interface graphique
						if (messageListener != null) {
							messageListener.onMessageReceived(message);
						}
					} else if (recu instanceof Key) {
						System.out.println(Arrays.toString(((Key) recu).getEncoded()));
						aes.key = (Key) recu;
					}
					// on affiche le message
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