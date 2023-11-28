import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;


// La classe Client qui peut être exécutée en mode console
public class Client  {

	private ObjectInputStream sInput;		// pour lire du socket
	private ObjectOutputStream sOutput;		// pour ecrire sur le socket
	private Socket socket;					// socket object

	private final String server;
	private String username;	// server et username
	private final int port;					// port

	private final AES aes; 				// clé de cryptage AES
	private String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/*
	 * Constructeur appelé par la console
	 * server: le serveur
	 * port: le port
	 * username: le nom d'utilisateur
	 */

	Client(String server, int port, String username) throws NoSuchPaddingException, NoSuchAlgorithmException {
		this.server = server;
		this.port = port;
		this.username = username;
		this.aes=new AES();
	}

	/*
	 * Pour demarrer le chat
	 */
	public boolean start() {
		// essai de se connecter au serveur
		try {
			socket = new Socket(server, port);
		}
		// exception si echec
		catch(Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}

		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);

		/*
		 * Creation des flux d'entree et de sortie
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

		// création du thread pour ecouter le serveur
		new ListenFromServer().start();
		// envoi du nom d'utilisateur au serveur en tant que String. Tous les autres messages seront des objets ChatMessage et non des String.
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


	/*
	 * Pour afficher un message
	 */
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
			msg.setMessage((Arrays.toString(aes.encrypt(msg.getMessage()))));
			System.out.println(msg.getMessage());
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * Si le client se deconnecte, on ferme les flux et le socket
	 */
	private void disconnect() {
		try {
			if(sInput != null) sInput.close();
		}
		catch(Exception ignored) {}
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception ignored) {}
		try{
			if(socket != null) socket.close();
		}
		catch(Exception ignored) {}

	}

	/*
	 * Si le portNumber n'est pas spécifié, 1500 est utilisé
	 * Si le serverAddress n'est pas spécifié, "localHost" est utilisé
	 * Si le nom d'utilisateur n'est pas spécifié, "Anonymous" est utilisé
	 */
	public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {
		// valeurs par défaut si pas d'arguments
		int portNumber = 1500;
		String serverAddress = "localhost";
		String userName = "Anonymous";
		Scanner scan = new Scanner(System.in);

		System.out.println("Enter the username: ");
		userName = scan.nextLine();

		// instanciation du client avec les valeurs par défaut ou celles spécifiées
		Client client = new Client(serverAddress, portNumber, userName);
		// test de la connexion au serveur, si echec, on quitte avec un return
		if(!client.start())
			return ;

		// si la connexion est ok, on affiche les instructions
		System.out.println("\nHello! Bienvenue sur l'espace de chat.");
		System.out.println("Instructions:");
		System.out.println("1. Tapez simplement un message pour l'envoyer à tous les utilisateurs connectés");
		System.out.println("2. Tapez @username votre_message pour envoyer un message privé à un utilisateur spécifique");
		System.out.println("Attention à bien respecter l'espace entre le nom d'utilisateur et le message.");
		System.out.println("3. Tapez USERS pour voir la liste des utilisateurs connectés");
		System.out.println("4. Tapez bye pour déconnecter du serveur");

		// boucle infinie pour lire le message de l'utilisateur et l'envoyer au serveur
		while(true) {
			System.out.print("> ");
			// lire le message de l'utilisateur
			String msg = scan.nextLine();
			//  message pour quitter le chatroom
			if(msg.equalsIgnoreCase("bye")) {
				client.sendMessage(new Message(Message.bye, ""));
				break;
			}
			// message pour voir la liste des utilisateurs connectés
			else if(msg.equalsIgnoreCase("USERS")) {
				client.sendMessage(new Message(Message.USERS, ""));
			}
			// message normal
			else {
				client.sendMessage(new Message(Message.MESSAGE, msg));
			}
		}
		// fermeture du scanner (lecture de l'entrée standard)
		scan.close();
		// deconnexion du client
		client.disconnect();
	}

	/*
	 * sous classe pour ecouter le serveur, en tant que thread, pour ne pas bloquer le client avec la lecture du socket
	 */
	class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					// lecture du message du serveur provenant du socket (sInput)
					Object recu = sInput.readObject();
					//Là je teste si recu est un String (un message normal) ou un Byte[] (la clé)
					if (recu instanceof String){
						System.out.println(recu);
					}
					else if (recu instanceof Key){
						System.out.println(Arrays.toString(((Key) recu).getEncoded()));
						aes.key= (Key) recu;
					}
					// on affiche le message
					System.out.print("> ");
				}
				catch(IOException e) {
					// notification
					String notif = " *** ";
					display(notif + "Server has closed the connection: " + e + notif);
					break;
				}
				catch(ClassNotFoundException ignored) {
				}
			}
		}
	}
}
