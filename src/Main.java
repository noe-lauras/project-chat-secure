public class Main {public static void main(String[] args) {
        // Création d'une instance de serveur
        Server server = new Server("localhost", 5000);

        // Création d'une instance de client
        Client client = new Client("localhost", 5000);

        // Démarrage du serveur dans un thread
        new Thread(() -> {
            server.start();
        }).start();

        // attente pour permettre au serveur de démarrer
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Démarrage du client dans un thread
        new Thread(() -> {
            client.start();
        }).start();

        // attente pour permettre au client de se connecter
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // chacun a leur tour, le client et le serveur envoient un message a l'autre avec des inputs clavier
        while (server.isRunning() && client.isRunning()) {
            System.out.println("Enter a message to send to the server:");
            String message = System.console().readLine();
            server.sendMessage(message);

            System.out.println("Enter a message to send to the client:");
            message = System.console().readLine();
            client.sendMessage(message);
        }
    }
}