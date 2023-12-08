import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.security.NoSuchAlgorithmException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class ClientGUI extends JFrame implements MessageListener {
    private final Client client;
    private final JTextField messageField;
    private final JTextArea chatArea;

    public ClientGUI(String username) throws NoSuchPaddingException, NoSuchAlgorithmException {

        // Initialisation de la fenêtre
        setTitle("Secure Chat Client");
        // quand on ferme la fenêtre, on déconnecte le client
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                deconnexion();
            }
        });
        setSize(600, 500);
        setLocationRelativeTo(null);

        // Création des composants GUI
        messageField = new JTextField();
        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JButton sendButton = new JButton("Envoyer");

        // on définit la taille du bouton
        sendButton.setPreferredSize(new Dimension(150, 50));

        sendButton.addActionListener(e -> {
            // Action à effectuer lorsqu'on clique sur le bouton "Send"
            sendMessage();
        });

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });


        // Organisation des composants dans la fenêtre
        setLayout(new BorderLayout());
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // création du client
        client = new Client(username);


        if (!client.start()) {
            // si le client n'a pas pu se connecter
            return;
        }

        // on implémente l'interface MessageListener pour recevoir les messages du serveur
        client.setMessageListener(this);

        // on affiche le message de bienvenue
        appendMessage("------------------------------------\n");
        appendMessage("Welcome " + username + "\n");
        appendMessage("Instructions:\n");
        appendMessage("1. Tapez simplement un message pour l'envoyer à tous les utilisateurs connectés\n");
        appendMessage("2. Tapez @username votre_message pour envoyer un message privé à un utilisateur spécifique\n");
        appendMessage("Attention à bien respecter l'espace entre le nom d'utilisateur et le message.\n");
        appendMessage("3. Tapez USERS pour voir la liste des utilisateurs connectés\n");
        appendMessage("4. Tapez bye pour déconnecter du serveur\n");
        appendMessage("------------------------------------\n");

        // Couleurs Discord-inspired
        Color discordBackground = new Color(54, 57, 63); // Fond sombre
        Color discordText = new Color(255, 255, 255); // Texte blanc
        Color discordGreen = new Color(0, 255, 0); // Couleur de texte pour les messages du serveur
        Color discordGray = new Color(169, 184, 204); // Couleur de texte pour les messages du client

        // Appliquer les couleurs
        getContentPane().setBackground(discordBackground);
        chatArea.setBackground(discordBackground);
        chatArea.setForeground(discordText);
        chatArea.setCaretColor(discordText);
        chatArea.setSelectionColor(discordGray);

        messageField.setBackground(discordBackground);
        messageField.setForeground(discordText);
        messageField.setCaretColor(discordText);
        messageField.setBorder(BorderFactory.createLineBorder(discordGray));

        sendButton.setBackground(discordBackground);
        sendButton.setForeground(discordText);
        sendButton.setBorder(BorderFactory.createLineBorder(discordGray));

        bottomPanel.setBackground(discordBackground);


        setVisible(true);

    }

    public void sendMessage() {
        // on récupère le message
        String msg = getMessage();
        if (msg.equalsIgnoreCase("bye")) {
            // si le message est "bye", on envoie un message de déconnexion au serveur
            client.sendMessage(new Message(Message.bye, ""));
            deconnexion();

        }
        else if (msg.equalsIgnoreCase("USERS")) {
            // si le message est "USERS", on envoie un message pour voir la liste des utilisateurs connectés
            client.sendMessage(new Message(Message.USERS, ""));
        }
        else {
            // sinon, on envoie un message normal
            client.sendMessage(new Message(Message.MESSAGE, msg));
        }
        // on efface le champ de message
        clearMessage();
    }

    public void appendMessage(String message) {
        // Ajouter le message à la zone de chat
        chatArea.append(message + "\n");
        // on fait défiler la zone de chat pour voir le nouveau message
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public String getMessage() {
        // Obtenir le texte du champ de message
        return messageField.getText();
    }

    public void clearMessage() {
        // Effacer le champ de message après l'envoi
        messageField.setText("");
    }

    private void deconnexion() {
        // déconnexion du client
        client.disconnect();
        System.exit(0);
    }

    @Override
    public void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> {
            // on affiche le message reçu
            appendMessage(message);
        });
    }
}
