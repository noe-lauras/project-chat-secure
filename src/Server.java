import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


public class Server {
	///Attribut de la classe Server
	AES aes = new AES();

	// unique id pour chaque client, plus facile pour la déconnexion
	private int uniqueId;
	// ArrayList pour la liste des clients connectés
	private final ArrayList<ClientThread> al;
	// affichage de l'heure et de la date
	private final SimpleDateFormat sdf;

	//Pour pouvoir fermer le thread d'écoute depuis la fonction turn_off
	DatagramSocket udpSocket;
	//Pour forcer la fermeture du serveur
	ServerSocket serverSocket;
	//  port de connection
	private final int DEFAULT_PORT=1500;
	// boolean pour savoir si le serveur est actif
	private volatile boolean estActif=true;
	//Pour que chaque thread voie la dernière valeur écrite, on met la variable en volatile
	//Comme en arduino
	private volatile boolean keepGoing=true;
	// notification
	private final String notif = " *** ";
	///

	//le constructeur ne reçoit que le port à écouter pour la connection en paramètre
	public Server() throws NoSuchPaddingException, NoSuchAlgorithmException {
		// format pour la date 
		sdf = new SimpleDateFormat("HH:mm:ss");
		// ArrayList pour la liste des clients connectés
		al = new ArrayList<>();
	}
	//Fonction qui attend qu'un client ping
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
	public void start() {
		// Démarre le thread pour écouter les requêtes UDP
    	new Thread(this::pong).start();
		//creation du socket serveur et ecoute sur le port
		try 
		{
			// le socket serveur 
			serverSocket = new ServerSocket(DEFAULT_PORT);

			// boucle infinie pour attendre les connexions des clients
			//System.out.println("avant boucle: "+estActif);
			while(estActif) 
			{
				//System.out.println("dans boucle : "+estActif);
				display("Server waiting for Clients on port " + DEFAULT_PORT + ".");
				// accepte la connection si le client est connecté
				Socket socket = serverSocket.accept();
				// creation d'un thread pour le client
				ClientThread t = new ClientThread(socket);
				// ajout du client à la liste des clients
				al.add(t);
				// on démarre le thread
				t.start();
			}
			//System.out.println("LE SERV N'EST PLUS ACTIF");
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
	protected void turn_off() {
        // on change le boolean pour ne plus être actif
		//System.out.println("BAH JE SUIS LA NON????");
		estActif = false;
		keepGoing= false;
		try{
		if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
		}catch(IOException e){e.printStackTrace();}
		 // Ferme la socket, ce qui devrait interrompre socket.receive()
		if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close();
		for (ClientThread ct:al) {
			ct.stopClientThread();
		}
	}


	// pour supprimer un client de la liste des clients connectés (s'il se déconnecte avec un bye)
	synchronized void remove(int id) {
		//System.out.println("JE ME DECONNECTE");
		String disconnectedClient = "";
		// on itère sur la liste des clients connectés, pour trouver le client concerné
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// si on le trouve, on le supprime de la liste
			if(ct.id == id) {
				disconnectedClient = ct.getUsername();
				al.remove(i);
				ct.stopClientThread();
				break;
			}
		}
	broadcast(notif + " %s has left the chat room." + notif,disconnectedClient);
	 // Vérifie si la liste des clients est vide
	//System.out.println("Avant la boucle qui teste vide ou pas");
	if (al.isEmpty()) {
		//System.out.println("C'est vide donc je stoppe");
		turn_off(); // On arrête le serveur si aucun client n'est connecté
		}
	}

	// Affichage (display) de n'importe quel event dans la console
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	// diffuser (broadcast) un message à tous les clients connectés 
	private synchronized boolean broadcast(Object msg,String user) {
		String message;
		if (msg instanceof String){
			message=String.format((String) msg,user);
		}
		else{
			message=user+": "+aes.decrypt((byte[]) msg);
		}
		// ajouter l'heure au message
		String time = sdf.format(new Date());
		
		// on check si le message est un message privé
		String[] w = message.split(" ",3);
		
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
	 * Si le portNumber n'est pas spécifié, 1500 est utilisé
	 */
	public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException {
		// creation du serveur avec le port spécifié et on le démarre
		Server server = new Server();
		server.start();
	}

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

				broadcast(notif + "%s has joined the chat room." + notif,username);

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
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					e2.printStackTrace();
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
			shut();
		}

		private void shut() {
			System.out.println("on ferme boutique");
			try {
				if (sOutput != null) sOutput.close();
				if (sInput != null) sInput.close();
				if (socket != null) {
					System.out.println("normalement on arrive jusque là");
					socket.close();
				}
			}catch (Exception ignored) {}
		}

		public void stopClientThread() {
		keepGoing = false;
		shut();
		}

        // écrire un message dans le flux de sortie du client (sOutput, ObjectOutputStream)
		private boolean writeMsg(String msg) {
			// on check si le socket est connecté, si non on ferme le flux de sortie 
			if(!socket.isConnected()) {
				shut();
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
