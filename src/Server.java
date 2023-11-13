import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class Server {
	// unique id pour chaque client, plus facile pour la déconnexion
	private static int uniqueId;
	// ArrayList pour la liste des clients connectés
	private ArrayList<ClientThread> al;
	// affichage de l'heure et de la date
	private SimpleDateFormat sdf;
	//  port de connection
	private int port;
	// boolean pour savoir si le serveur est actif
	private boolean estActif;
	// notification
	private String notif = " *** ";
	
	//le constructeur ne reçoit que le port à écouter pour la connection en paramètre
	
	public Server(int port) {
		// port
		this.port = port;
		// format pour la date 
		sdf = new SimpleDateFormat("HH:mm:ss");
		// ArrayList pour la liste des clients connectés
		al = new ArrayList<ClientThread>();
	}
	
	public void start() {
		estActif = true;
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
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					// on ferme les flux de sortie et d'entrée ainsi que le socket de chaque client
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
					}
				}
			}
			catch(Exception e) {
				display("Exception, fermeture du server and clients: " + e);
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
        /*
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
		}
         */
	}
	
	// Affichage (display) de n'importe quel event dans la console
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	// diffuser (broadcast) un message à tous les clients connectés 
	private synchronized boolean broadcast(String message) {
		// ajouter l'heure au message
		String time = sdf.format(new Date());
		
		// on check si le message est un message privé
		String[] w = message.split(" ",3);
		
		boolean isPrivate = false;
		if(w[1].charAt(0)=='@') 
			isPrivate=true;
		
		
		// le message est privé, on l'envoie au client concerné
		if(isPrivate==true)
		{
			String tocheck=w[1].substring(1, w[1].length());
			
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
			if(found!=true)
			{
				return false; 
			}
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

	// pour supprimer un client de la liste des clients connectés (si il se déconnecte avec un bye)
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
		broadcast(notif + disconnectedClient + " has left the chat room." + notif);
	}
	
	/*
	 * Si le portNumber n'est pas spécifié, 1500 est utilisé
	 */ 
	public static void main(String[] args) {
		int portNumber = 1500;
		// creation du serveur avec le port spécifié et on le démarre
		Server server = new Server(portNumber);
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
		ClientThread(Socket socket) {
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
				broadcast(notif + username + " has joined the chat room." + notif);
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString() + "\n";
		}
		
		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		// boucle infinie pour écouter les messages des clients
		public void run() {
            // boucler jusqu'à ce que le client se déconnecte, (avec un bye)
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
				String message = cm.getMessage();

				// on check le type du message, pour traiter les cas : USERS, MESSAGE, bye
				switch(cm.getType()) {
                // MESSAGE pour un message normal
				case Message.MESSAGE:
					boolean confirmation =  broadcast(username + ": " + message);
					if(confirmation==false){
						String msg = notif + "Sorry. No such user exists." + notif;
						writeMsg(msg);
					}
					break;
                // bye pour se déconnecter
				case Message.bye:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
                // USERS pour la liste des utilisateurs connectés
				case Message.USERS:
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					// on itère sur la liste des clients connectés, pour envoyer la liste à l'utilisateur
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
					}
					break;
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
