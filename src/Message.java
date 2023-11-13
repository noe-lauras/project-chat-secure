import java.io.*;
/*
 * Cette classe définit le type de message qui sera envoyé par le Client.
 * Elle implémente l'interface Serializable pour pouvoir être envoyée par le socket.
 * 
 * Les différents types de message sont:
 *    - USERS pour la liste des utilisateurs connectés 
 *    - MESSAGE pour un message normal
 *    - bye pour se déconnecter
 * 
 * Le constructeur prend en paramètre le type de message et le message lui-même.
 */

public class Message implements Serializable {

	static final int USERS = 0, MESSAGE = 1, bye = 2;
	private int type;
	private String message;
	
	// constructeur
	Message(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	int getType() {
		return type;
	}

	String getMessage() {
		return message;
	}
}