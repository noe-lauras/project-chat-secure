import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


public class Server {
	AES aes = new AES();

	// unique id pour chaque client, plus facile pour la déconnexion
	private static int uniqueId;
	// ArrayList pour la liste des clients connectés
	private final ArrayList<ClientThread> al;
	// affichage de l'heure et de la date
	private final SimpleDateFormat sdf;
	//  port de connection
	private final int port;
	// boolean pour savoir si le serveur est actif
	private boolean estActif;
	private static boolean keepGoing=true;
	// notification
	private final String notif = " *** ";

	//le constructeur ne reçoit que le port à écouter pour la connection en paramètre

	public Server(int port) throws NoSuchPaddingException, NoSuchAlgorithmException {
		// port
		this.port = port;
		// format pour la date
		sdf = new SimpleDateFormat("HH:mm");
		// ArrayList pour la liste des clients connectés
		al = new ArrayList<>();
	}
	//Fonction qui attend qu'un client ping
	public static void pong(){
		int receivePort = 1500; // Port pour recevoir les messages
		int sendPort = 1501; // Port pour envoyer les réponses

		try {
			DatagramSocket socket = new DatagramSocket(receivePort);
			//System.out.println("En attente de messages...");
			while (keepGoing) {
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
			socket.close();
			// Ajouter la fermeture de la socket ici si nécessaire, par exemple sur un signal de sortie.
		} catch (IOException ignored) {
		}
	}
	public void start() {
		estActif = true;
		// Démarre le thread pour écouter les requêtes UDP
		new Thread(Server::pong).start();
		//creation du socket serveur et ecoute sur le port
		try
		{
			// le socket serveur
			ServerSocket serverSocket = new ServerSocket(port);

			// boucle infinie pour attendre les connexions des clients
			while(estActif)
			{
				display("Server waiting for Clients on port " + port + ".");
				// accepte la connection si le client est connecté
				Socket socket = serverSocket.accept();
				// on casse la boucle si le serveur n'est plus actif
				if(!estActif)
					break;
				// creation d'un thread pour le client
				ClientThread t = new ClientThread(socket);
				// ajout du client à la liste des clients
				al.add(t);
				// on démarre le thread
				t.start();
			}
			// si on n'est plus actif on ferme le serveur
			try {
				serverSocket.close();
				for (ClientThread tc : al) {
					try {
						// on ferme les flux de sortie et d'entrée ainsi que le socket de chaque client
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ignored) {
					}
				}
			}
			catch(Exception e) {
				display("Exception, fermeture du serveur et déconnexion des clients: " + e);
			}
		}
		catch (IOException e) {
			String msg = sdf.format(new Date()) + " Exception sur le ServerSocket: " + e + "\n";
			display(msg);
		}
	}

	// pour stopper le serveur
	protected void stop() {
		// on change le boolean pour ne plus être actif
		estActif = false;
	}

	// Affichage (display) de n'importe quel event dans la console
	private void display(String msg) {
		String time = sdf.format(new Date());
		String formattedMsg = String.format("| %s | %s", time, msg);
		System.out.println(formattedMsg);
	}

	// diffuser (broadcast) un message à tous les clients connectés
	private synchronized boolean broadcast(Object msg, String user) {
		String time = sdf.format(new Date());
		String message;
		if (msg instanceof String) {
			message = String.format("%s | %s", user, msg);
		} else {
			message = String.format("%s | %s", user, aes.decrypt((byte[]) msg));
		}
		// on check si le message est un message privé
		String[] w = message.split(" ", 3);
		boolean isPrivate = w[1].charAt(0) == '@';


		// le message est privé, on l'envoie au client concerné
		if(isPrivate)
		{
			String tocheck=w[1].substring(1);

			message=w[0]+w[2];
			String messageLf = time + " " + message + "\n";
			boolean found=false;
			// on itère sur la liste des clients connectés, pour trouver le client concerné
			for(int y=al.size(); --y>=0;)
			{
				ClientThread ct1=al.get(y);
				String check=ct1.getUsername();
				if(check.equals(tocheck))
				{
					// on essaye d'envoyer le message au client, si ça ne marche pas on le supprime de la liste
					// --> ça veut dire qu'il n'est plus connecté
					if(!ct1.writeMsg(messageLf)) {
						al.remove(y);
						display("Disconnected Client " + ct1.username + " removed from list.");
					}
					// on a trouvé le client, on sort de la boucle
					found=true;
					break;
				}
			}
			// le client n'existe pas
			return found;
		}
		// le message n'est pas privé, on l'envoie à tous les clients
		else
		{
			String messageLf = time + " " + message + "\n";
			// on affiche le message dans la console
			System.out.print(messageLf);
			// on itère sur la liste des clients connectés, pour envoyer le message à chacun
			for(int i = al.size(); --i >= 0;) {
				ClientThread ct = al.get(i);
				// on essaye d'envoyer le message au client, si ça ne marche pas on le supprime de la liste
				// --> ça veut dire qu'il n'est plus connecté
				if(!ct.writeMsg(messageLf)) {
					al.remove(i);
					display("Disconnected Client " + ct.username + " removed from list.");
				}
			}
		}
		return true;
	}

	// pour supprimer un client de la liste des clients connectés (s'il se déconnecte avec un bye)
	synchronized void remove(int id) {
		String disconnectedClient = "";
		// on itère sur la liste des clients connectés, pour trouver le client concerné
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// si on le trouve, on le supprime de la liste
			if(ct.id == id) {
				disconnectedClient = ct.getUsername();
				al.remove(i);
				break;
			}
		}
		broadcast(notif + " " + disconnectedClient + " has left the chat room." + notif, "Server");
	}

	/*
	 * Si le portNumber n'est pas spécifié, 1500 est utilisé
	 */


	// un thread pour chaque client
	class ClientThread extends Thread {
		// le socket du client
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// son unique id
		int id;
		// le nom d'utilisateur
		String username;
		// le message est un objet pour pouvoir envoyer le type du message (MESSAGE, USERS, bye)
		Message cm;
		// la date
		String date;

		// Constructeur
		ClientThread(Socket socket) throws IOException {
			// unique id, on l'incrémente à chaque nouveau client connecté
			id = ++uniqueId;
			this.socket = socket;
			// on essaye de créer les flux d'entrée et de sortie, si ça ne marche pas on affiche une erreur
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// on lit le nom d'utilisateur
				username = (String) sInput.readObject();

				broadcast(notif + username+ " has joined the chat room." + notif,"Server");

				// si aes n'est pas instancié, on le fait
				if (aes.key == null) {
					try {
						aes.genereKey();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				sendAESKey();
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException ignored) {
			}
			date = new Date() + "\n";
		}
		private void sendAESKey() {
			try {
				System.out.println(Arrays.toString(aes.key.getEncoded()));
				sOutput.writeObject(aes.key); // Envoie la clé AES au client
			} catch(IOException e) {
				display("Error sending AES key to " + username);
				e.printStackTrace();
			}
		}


		public String getUsername() {
			return username;
		}

		// boucle infinie pour écouter les messages des clients
		public void run() {
			// boucler jusqu'à ce que le client se déconnecte, (avec un bye)
			keepGoing = true;
			while(keepGoing) {
				// lire le message envoyé par le client, on cast en Message car c'est un objet
				try {
					cm = (Message) sInput.readObject();
				}
				catch (IOException e) {
					//display(username + " Exception reading Streams: " + e);
					break;
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// on récupère le message de l'objet Message
				Object message = cm.getMessage();
//				if (message instanceof byte[]){
//					System.out.println("MES COUILLES A SKI");
//				}
				// on check le type du message, pour traiter les cas : USERS, MESSAGE, bye
				switch (cm.getType()) {
					// MESSAGE pour un message normal
					case Message.MESSAGE -> {
						boolean confirmation = broadcast(message,username);
						if (!confirmation) {
							String msg = notif + "Sorry. No such user exists." + notif;
							writeMsg(msg);
						}
					}
					// bye pour se déconnecter
					case Message.bye -> {
						display(username + " disconnected with a bye message.");
						keepGoing = false;
					}
					// USERS pour la liste des utilisateurs connectés
					case Message.USERS -> {
						writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
						// on itère sur la liste des clients connectés, pour envoyer la liste à l'utilisateur
						for (int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
							writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
						}
					}
				}
			}
			// si on sort de la boucle, on déconnecte le client de la liste des clients connectés
			remove(id);
			close();
		}

		private void close() {
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception ignored) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception ignored) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception ignored) {}
		}

		// écrire un message dans le flux de sortie du client (sOutput, ObjectOutputStream)
		private boolean writeMsg(String msg) {
			// on check si le socket est connecté, si non on ferme le flux de sortie
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// on écrit le message dans le flux de sortie
			try {
				sOutput.writeObject(msg);
			}
			// si ça ne marche pas on informe l'utilisateur que le message n'a pas été envoyé
			catch(IOException e) {
				display(notif + "Error sending message to " + username + notif);
				display(e.toString());
			}
			return true;
		}
	}
}