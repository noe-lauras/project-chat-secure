import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.*;

/*
* La classe Server est le serveur du chat. Il écoute les connexions des clients et leur envoie les messages.
* Il a besoin d'un thread pour écouter chaque client en permanence.
*
* attributs:
*
* 	- port: le port de connexion
* 	- publicKeys: le dictionnaire des clés publiques des clients
* 	- dhKey: la clé de Diffie-Hellman
* 	- uniqueId: l'id unique pour chaque client
* 	- al: la liste des clients connectés
* 	- sdf: pour afficher la date
* 	- serverSocket: le socket du serveur
* 	- keepGoing: pour savoir si le serveur est actif
* 	- notif: symboles pour les notifications
*
* méthodes:
* 	- Server: constructeur
* 	- start: pour démarrer le serveur
* 	- stop: pour arrêter le serveur
* 	- display: pour afficher un message dans la console, avec la date
* 	- broadcast: pour envoyer un message à tous les clients
* 	- remove: pour supprimer un client de la liste des clients connectés
* 	- ClientThread: sous classe pour écouter les messages des clients
	* 	- sendDHKey: pour envoyer la clé DH au client
	* 	- run: pour démarrer le thread
	* 	- getUsername: pour récupérer le nom d'utilisateur
	* 	- close: pour fermer les flux d'entrée et de sortie ainsi que le socket
	* 	- writeMsg: pour écrire un message dans le flux de sortie du client (sOutput, ObjectOutputStream)
	*
 */
public class Server {
	private int port = 1500;
	private final HashMap<String, PublicKey> publicKeys = new HashMap<>();
	private DHKey dhKey;
	private int uniqueId;
	private final ArrayList<ClientThread> al;
	private final SimpleDateFormat sdf;
	ServerSocket serverSocket;
	private volatile boolean keepGoing = true;
	private final String notif = " *** ";

	private DatagramSocket udpSocket;

	/*
	 * Constructeur:
	 * Il prend en paramètre le port de connexion
	 * On initialise les attributs
	 * On génère la clé publique et privée de Diffie-Hellman : dhKey.generateKeys()
	 *
	 */
	public Server(int port) throws NoSuchPaddingException, NoSuchAlgorithmException {
		dhKey = new DHKey();
		dhKey.generateKeys();
		// format pour la date
		sdf = new SimpleDateFormat("HH:mm");
		al = new ArrayList<>();
		this.port = port;
	}

	public void pong(){
        int receivePort = 1500; // Port pour recevoir les messages
        int sendPort = 1501; // Port pour envoyer les réponses

        try {
            udpSocket = new DatagramSocket(receivePort);
            //System.out.println("En attente de messages...");
            while (keepGoing) {
                //System.out.println("En attente de recevoir un paquet. keepGoing = " + keepGoing);
                if (udpSocket.isClosed()) {
                    System.out.println("La socket UDP est fermée.");
                }
                //Pour recevoir le message envoyé par le ping de Client
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket);
                InetAddress clientAddress = receivePacket.getAddress();
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                //

                if (message.equals("Serveur je te parle")) {
                    DatagramSocket responseSocket = new DatagramSocket();
                    String responseMessage = "Client je te réponds";
                    byte[] sendData = responseMessage.getBytes();
                    //On récupère l'ip du destinataire pour lui renvoyer un message
                    InetAddress destinationAddress = InetAddress.getByName(clientAddress.getHostAddress());
                    //Message que l'on renvoie
                    DatagramPacket responsePacket = new DatagramPacket(sendData, sendData.length, destinationAddress, sendPort);
                    responseSocket.send(responsePacket);
                    responseSocket.close();
                    //System.out.println("Message envoyé avec succès !");
                }
            }
            //System.out.println("YA PU PERSONNE");

            //On ferme la socket (normalement, c'est déjà geré ailleurs mais par précaution...)
            udpSocket.close();
        } catch (IOException ignored) {
        }
    }

	/*
	 * start: pour démarrer le serveur
	 * On se sert d'un boolean keepGoing pour savoir si le serveur est actif
	 * Tant que keepGoing est vrai :
		 * On crée un socket pour écouter les connexions des clients
		 * On crée un thread pour chaque client qui se connecte
		 * On ajoute le thread à la liste des clients connectés
		 * On démarre le thread
	 * Quand on sort de la boucle,
	 * On ferme le socket du serveur
	 * On ferme les flux d'entrée et de sortie de chaque client
	 * On ferme le socket de chaque client
	 */
	public void start() {
		// Démarre le thread pour écouter les requêtes UDP
        new Thread(this::pong).start();

		keepGoing=true;
		try
		{ serverSocket = new ServerSocket(port);
			while(keepGoing)
			{
				display("Server waiting for Clients on port " + port + ".");
				Socket socket = serverSocket.accept();
				if(!keepGoing){
					break;
				}
				ClientThread t = new ClientThread(socket);
				al.add(t);
				t.start();
			}
			try {
				serverSocket.close();
				for (int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					}
					catch(IOException ignored) {}
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

	/*
	 * stop: pour arrêter le serveur
	 * On met le boolean keepGoing à false, pour sortir de la boucle while de la méthode start
	 */
	protected void stop() {
			keepGoing = false;
	}

	/*
	 * display: pour afficher un message dans la console, avec la date
	 * On utilise le format sdf pour la date : HH:mm
	 * HH désigne les heures, mm les minutes
	 */
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}

	/*
	 * broadcast: pour envoyer un message à tous les clients
	 * on la déclare synchronized pour éviter les conflits entre les threads :
	 *  → un seul thread peut y accéder à la fois
	 * Si le message est un String ( message non crypté ) :
	 * → on le formate avec le nom d'utilisateur
	 * Sinon, c'est un message crypté :
	 * → on récupère la clé publique du client qui à envoyé le message
	 * → on la donne à la clé de Diffie-Hellman pour qu'elle puisse générer la clé secrète commune
	 * → on déchiffre le message avec la clé secrète commune
	 * On ajoute l'heure au message
	 *
	 * Puis, on regarde dans quel cas on est :
	 * 1) le message est privé :
	 * → on récupère le nom d'utilisateur du client qui doit recevoir le message
	 * → on itère sur la liste des clients connectés, pour trouver le client concerné et le client qui envoie le message
	 * → on essaye d'envoyer le message au client, si ça ne marche pas on le supprime de la liste
	 *
	 * 2) le message n'est pas privé :
	 * → on itère sur la liste des clients connectés, pour envoyer le message à chacun
	 * → on essaye d'envoyer le message au client, si ça ne marche pas on le supprime de la liste, ça veut dire qu'il n'est plus connecté
	 *
	 * On retourne true si le message a été envoyé, false sinon
	 */
	private synchronized boolean broadcast(Object msg,String user) {
		String message;
		if (msg instanceof String){
			message=String.format((String) msg,user);
		}
		else{
			PublicKey publicKey = publicKeys.get(user);
			this.dhKey.setReceivedPublicKey(publicKey);
			this.dhKey.generateCommonSecretKey();
			message = user + " : " + dhKey.decrypt((byte[]) msg);
		}
		String time = sdf.format(new Date());
		// check si le message est privé
		String[] w = message.split(" ",3);
		boolean isPrivate = w[1].charAt(0) == '@';

		// le message est privé, on l'envoie au client concerné
		if(isPrivate)
		{
			String tocheck=w[1].substring(1);
			message=w[0]+w[2];
			String messageLf = time + " " + message + "\n";
			boolean found=false;
			// on itère sur la liste des clients connectés, pour trouver le client concerné et le client qui envoie le message
			for(int y=al.size(); --y>=0;)
			{
				ClientThread ct1=al.get(y);
				String check=ct1.getUsername();
				if(check.equals(tocheck) || check.equals(user))
				{
					// on essaye d'envoyer le message au client, si ça ne marche pas on le supprime de la liste
					// --> ça veut dire qu'il n'est plus connecté
					if(!ct1.writeMsg(messageLf)) {
						al.remove(y);
						display("Disconnected Client " + ct1.username + " removed from list.");
					}
					found=true;
				}
			}
			// le client n'existe pas
			return found;
		}
		// le message n'est pas privé, on l'envoie à tous les clients
		else
		{
			String messageLf = time + " " + message + "\n";
			System.out.print(messageLf);
			// on itère sur la liste des clients connectés, pour envoyer le message à chacun
			for(int i = al.size(); --i >= 0;) {
				ClientThread ct = al.get(i);
				// on essaye d'envoyer le message au client, si ça ne marche pas on le supprime de la liste
				// → ça veut dire qu'il n'est plus connecté
				if(!ct.writeMsg(messageLf)) {
					al.remove(i);
					display("Disconnected Client " + ct.username + " removed from list.");
				}
			}
		}
		return true;
	}

	/*
	 * remove: pour supprimer un client de la liste des clients connectés
	 * on la déclare synchronized pour éviter les conflits entre les threads :
	 *  → un seul thread peut y accéder à la fois
	 * On récupère le nom d'utilisateur du client déconnecté
	 * On itère sur la liste des clients connectés, jusqu'à trouver le client concerné
	 * On le supprime de la liste
	 * On envoie un message à tous les clients, avec le broadcast, pour les informer que le client s'est déconnecté
	 * Puis, on check si on a supprimé le dernier client, si oui on arrête le serveur.
	 */
	synchronized void remove(int id) {
		String disconnectedClient = "";
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			if(ct.id == id) {
				disconnectedClient = ct.getUsername();
				al.remove(i);
				break;
			}
		}
		broadcast(notif + disconnectedClient + " has left the chat room." + notif, "Server");

		// si on a supprimé le dernier client, on arrête le serveur
		if (al.size() == 0) {
			display("No more clients connected, stopping server.");
			stop();
		}
	}

	/*
	* ClientThread: sous classe pour écouter les messages des clients
	* attributs:
	* 	- sInput: pour lire les messages du client
	* 	- sOutput: pour écrire sur le socket
	* 	- socket: le socket pour se connecter au client
	* 	- id: l'id du client
	* 	- username: le nom d'utilisateur
	* 	- cm: le message du client
	* 	- date: la date du message
	* méthodes:
	* 	- ClientThread: constructeur
	* 	- run: pour démarrer le thread
	* 	- getUsername: pour récupérer le nom d'utilisateur
	* 	- close: pour fermer les flux d'entrée et de sortie ainsi que le socket
	* 	- writeMsg: pour écrire un message dans le flux de sortie du client (sOutput, ObjectOutputStream)
	* 	- sendDHKey: pour envoyer la clé DH au client
	*
	 */
	class ClientThread extends Thread {
		PublicKey publicKeyClient;
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		int id;
		String username;
		Message cm;
		String date;

		/*
		 * Constructeur:
		 * Il prend en paramètre le socket du client
		 * On initialise les attributs
		 * On lit le nom d'utilisateur puis la clé publique du client
		 * Une fois la clé publique récupérée, on l'ajoute au dictionnaire des clés publiques.
		 * On envoie la clé publique du serveur au client, pour qu'il puisse générer la clé secrète commune par la suite
		 * On envoie un message à tous les clients, avec le broadcast, pour les informer qu'un nouveau client s'est connecté
		 */
		ClientThread(Socket socket) throws IOException {
			// unique id, on l'incrémente à chaque nouveau client connecté
			id = ++uniqueId;
			this.socket = socket;
			try
			{
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// on lit le nom d'utilisateur
				username = (String) sInput.readObject();
				// on recupère la clé publique du client
				publicKeyClient = (PublicKey) sInput.readObject();
				// on l'ajoute au dictionnaire des clés publiques
				publicKeys.put(username, publicKeyClient);
				broadcast(notif + username+ " has joined the chat room." + notif,"Server");
				sendDHKey();

			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException ignored) {
			}
			date = new Date() + "\n";
		}

		/*
		 * sendDHKey: pour envoyer la clé DH au client
		 * On récupère la clé publique du serveur
		 * On écrit la clé DH dans le flux de sortie du client
		 */
		private void sendDHKey() {
			try {
				sOutput.writeObject(dhKey.getPublicKey()); // Envoie la clé DH au client
			} catch(IOException e) {
				display("Error sending DH key to " + username);
				e.printStackTrace();
			}
		}

		/*
		 * getUsername: pour récupérer le nom d'utilisateur
		 */

		public String getUsername() {
			return username;
		}

		/*
		 * run: pour démarrer le thread
		* On boucle jusqu'à ce que le client se déconnecte, (avec un bye)
		* On lit le message envoyé par le client, on cast en Message car c'est un objet
		* On récupère le message de l'objet Message
		* On check le type du message, pour traiter les cas : USERS, MESSAGE, bye
		* 1. MESSAGE pour un message normal
		* → on envoie le message à tous les clients avec le broadcast
		* 2. bye pour se déconnecter
		* → on affiche un message de déconnexion, on a déjà traité le cas dans la méthode remove
		* 3. USERS pour la liste des utilisateurs connectés
		* → on envoie la liste des utilisateurs connectés au client
		* Quand on sort de la boucle, on déconnecte le client de la liste des clients connectés
		* */
		public void run() {
			boolean keepGoing = true;
			while(keepGoing) {
				// lire le message envoyé par le client, on cast en Message car c'est un objet
				try {
					cm = (Message) sInput.readObject();
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// on récupère le message de l'objet Message
				Object message = cm.getMessage();
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
						display(username + " disconnected with a LOGOUT message.");
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

		/*
		 * close: pour fermer les flux d'entrée et de sortie ainsi que le socket
		 */
		private void close() {
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * writeMsg: pour écrire un message dans le flux de sortie du client (sOutput, ObjectOutputStream)
		 * on la déclare boolean pour savoir si le message a été envoyé ou non
		 * on check si le socket est connecté, si non on ferme le flux de sortie
		 * Si le socket est connecté, on écrit simplement le message dans le flux de sortie sOutput avec la méthode writeObject
		 */
		private boolean writeMsg(String msg) {
			if(!socket.isConnected()) {
				close();
				return false;
			}
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