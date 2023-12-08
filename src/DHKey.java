import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;

public class DHKey {

    // PRINCIPE DE L'ALGORITHME DE DIFFIE-HELLMAN
    /*
        // 1. ------------------------------------------------------------------
            O                                        O
           /|\                                      /|\
           / \                                      / \

          Client                                    Serveur

        // 2. ------------------------------------------------------------------
           Le client et le serveur génèrent chacun une paire de clés (privée et publique) :
           dhKey.generateKeys();

            O                                        O
           /|\                                      /|\
           / \                                      / \

          Client                                    Serveur
            clé privée                                clé privée
            clé publique                              clé publique

        // 3. ------------------------------------------------------------------
              Le client envoie sa clé publique au serveur :
              client.sendPublicKey();

               O                                        O
              /|\                                      /|\
              / \                                      / \

             Client                                    Serveur
            clé privée                               clé privée
            clé publique                             clé publique
                                                     clé publique du client

        // 4. ------------------------------------------------------------------
                Le serveur envoie sa clé publique au client :
                server.sendPublicKey();

                 O                                        O
                /|\                                      /|\
                / \                                      / \

                 Client                                    Serveur
                clé privée                                clé privée
                clé publique                              clé publique
               clé publique du serveur                   clé publique du client

        // 5. ------------------------------------------------------------------
                Le client génère la clé secrète commune en utilisant la clé publique du serveur et sa clé privée :
                Le serveur génère la clé secrète commune en utilisant la clé publique du client et sa clé privée :
                client.generateCommonSecretKey();

                    O                                        O
                   /|\                                      /|\
                   / \                                      / \

                 Client                                    Serveur

                clé privée                                clé privée
                clé publique                              clé publique
                clé publique du serveur                   clé publique du client
                clé secrète commune                       clé secrète commune

        // 6. ------------------------------------------------------------------
                Le client chiffre le message avec la clé secrète commune et l'envoie au serveur :
                client.sendMessage();

                    O                                        O
                   /|\                                      /|\
                   / \                                      / \

                 Client                                    Serveur

                clé privée                                clé privée
                clé publique                              clé publique
                clé publique du serveur                   clé publique du client
                clé secrète commune                       clé secrète commune
                message chiffré par le client  ----->     message chiffré par le client

        // 7. ------------------------------------------------------------------
                Le serveur déchiffre le message avec la clé secrète commune :
                server.decryptMessage();

                    O                                        O
                   /|\                                      /|\
                   / \                                      / \

                 Client                                    Serveur

                clé privée                                clé privée
                clé publique                              clé publique
                clé publique du serveur                   clé publique du client
                clé secrète commune                       clé secrète commune
                message chiffré par le client  ----->     message chiffré par le client
                                                          message déchiffré par le serveur


     */


    //~ --- [CHAMPS D'INSTANCE] ---------------------------------------------------------------------------------------

    private PrivateKey privateKey;  // Clé privée du côté courant
    private PublicKey publicKey;    // Clé publique du côté courant
    private PublicKey receivedPublicKey;  // Clé publique reçue de l'autre côté
    private byte[] secretKey;       // Clé secrète partagée


    //~ --- [MÉTHODES] --------------------------------------------------------------------------------------------------



    /*
       1 .Création de l'objet Cipher avec l'algorithme AES :
            Cipher cipher = Cipher.getInstance("AES");
            Cette ligne crée un objet Cipher en utilisant l'algorithme de chiffrement AES. Cipher est la classe Java qui fournit les fonctionnalités de chiffrement et de déchiffrement.

       2. Création de la clé secrète partagée à partir du tableau de bytes :
            Key secretKeySpec = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");
            On utilise la classe SecretKeySpec pour créer une clé secrète (Key) à partir du tableau de bytes secretKey. Cela sert à représenter la clé secrète sous une forme que l'algorithme de chiffrement comprend.

       3. Initialisation du Cipher en mode de déchiffrement avec la clé secrète :
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            On initialise l'objet Cipher en mode de chiffrement (Cipher.ENCRYPT_MODE) et on lui fournit la clé secrète (secretKeySpec).

       4. Déchiffrement du message :
            return cipher.doFinal(plainText.getBytes());
            On utilise la méthode doFinal pour chiffrer le tableau de bytes plainText.getBytes(). Le résultat est stocké dans encryptedBytes.

       5. Conversion des bytes déchiffrés en une chaîne :
            return new String(encryptedBytes);
            Enfin, les bytes chiffrés sont convertis en une chaîne de caractères à l'aide du constructeur de String.

     */
    public String decrypt(byte[] encryptedMessage) {
        try {
            // Création de l'objet Cipher avec l'algorithme AES
            Cipher cipher = Cipher.getInstance("AES");

            // Création de la clé secrète partagée à partir du tableau de bytes
            Key secretKeySpec = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");

            // Initialisation du Cipher en mode de déchiffrement avec la clé secrète
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            // Déchiffrement du message
            byte[] decryptedBytes = cipher.doFinal(encryptedMessage);

            // Conversion des bytes déchiffrés en une chaîne
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Chiffre le message avec la clé secrète partagée et envoie le message à l'autre côté pour déchiffrement.
     *
     * @param msg Message à chiffrer et envoyer.
     */

    /*

    1. Vérification du type du message :
        if (msg.getMessage() instanceof String) {
            String plainText = (String) msg.getMessage();
            ...
    La méthode commence par vérifier si le message contenu dans l'objet msg est une instance de la classe String.

    2. Extraction du texte du message :
        String plainText = (String) msg.getMessage();
        Si le message est une chaîne de caractères, on l'extrait en utilisant une conversion de type.

    3. Création de l'objet Cipher avec l'algorithme AES :
        Cipher cipher = Cipher.getInstance("AES");
        Comme dans la méthode decrypt, on crée un objet Cipher en utilisant l'algorithme de chiffrement AES.

    4. Création de la clé secrète partagée à partir du tableau de bytes :
        Key secretKeySpec = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");
        On utilise SecretKeySpec pour créer une clé secrète (Key) à partir du tableau de bytes secretKey.

    5. Initialisation du Cipher en mode de chiffrement avec la clé secrète :
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        On initialise l'objet Cipher en mode de chiffrement (Cipher.ENCRYPT_MODE) et on lui fournit la clé secrète (secretKeySpec).

    6. Chiffrement du message :
        return cipher.doFinal(plainText.getBytes());
        On chiffre le texte (plainText) en utilisant la méthode doFinal de l'objet Cipher, et on retourne le résultat sous forme de tableau de bytes.

     */

    public byte[] encrypt(Message msg) {
        try {
            if (msg.getMessage() instanceof String) {
                String plainText = (String) msg.getMessage();

                // Création de l'objet Cipher avec l'algorithme AES
                Cipher cipher = Cipher.getInstance("AES");

                // Création de la clé secrète partagée à partir du tableau de bytes
                Key secretKeySpec = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");

                // Initialisation du Cipher en mode de chiffrement avec la clé secrète
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

                // Chiffrement du message
                return cipher.doFinal(plainText.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //~ ------------------------------------------------------------------------------------------------------------------

    /**
     * la méthode generateCommonSecretKey() effectue les étapes suivantes :
     *
     *      * 1. Initialisation de l'objet KeyAgreement avec la clé privée
     *      * 2. Étape du protocole Diffie-Hellman pour générer la clé secrète commune
     *      * 3. Génération de la clé secrète commune non tronquée
     *      * 4. Utilisation d'une fonction de hachage (par exemple SHA-256) pour dériver la clé
     *      * 5. Tronquer la clé à la longueur requise par AES (128 bits)
     *
     * Initialisation de l'objet KeyAgreement :
     *          On initialise l'objet KeyAgreement avec l'algorithme Diffie-Hellman (DH) et la clé privée (privateKey). Cela prépare l'objet KeyAgreement à effectuer l'échange de clés selon le protocole Diffie-Hellman.
     * Étape du protocole Diffie-Hellman :
     *          keyAgreement.doPhase(receivedPublicKey, true);
     *          Cette ligne exécute une phase du protocole Diffie-Hellman. Elle prend en paramètre la clé publique reçue de l'autre partie (receivedPublicKey) et indique que c'est la dernière phase (true). Cela génère une clé secrète commune qui sera utilisée pour chiffrer les données échangées.
     * Génération de la clé secrète commune non tronquée :
     *          byte[] fullSecretKey = keyAgreement.generateSecret();
     *          Une fois la phase de Diffie-Hellman complétée, cette ligne génère la clé secrète commune sous forme de tableau de bytes non tronquée.
     * Utilisation d'une fonction de hachage (SHA-256) :
     *          MessageDigest sha = MessageDigest.getInstance("SHA-256");
     *          secretKey = sha.digest(fullSecretKey);
     *          La clé secrète commune non tronquée est ensuite passée à une fonction de hachage (dans ce cas SHA-256) pour produire une empreinte unique qui servira de clé de chiffrement.
     * Tronquer la clé à la longueur requise par AES (128 bits) :
     *          secretKey = Arrays.copyOf(secretKey, 16);
     *
     * Enfin, la clé générée par la fonction de hachage (qui pourrait être plus longue que nécessaire) est tronquée à la longueur requise pour le chiffrement AES (Advanced Encryption Standard). Dans ce cas, la longueur est de 16 bytes (128 bits).
     *
     *
     */
    public void generateCommonSecretKey() {
        try {
            // Initialisation de l'objet KeyAgreement avec la clé privée
            final KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(privateKey);

            // Étape du protocole Diffie-Hellman pour générer la clé secrète commune
            keyAgreement.doPhase(receivedPublicKey, true);

            // Génération de la clé secrète commune non tronquée
            byte[] fullSecretKey = keyAgreement.generateSecret();

            // Utilisation d'une fonction de hachage (par exemple SHA-256) pour dériver la clé
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            secretKey = sha.digest(fullSecretKey);

            // Tronquer la clé à la longueur requise par AES (128 bits)
            secretKey = Arrays.copyOf(secretKey, 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //~ ------------------------------------------------------------------------------------------------------------------

    public void setReceivedPublicKey(PublicKey receivedPublicKey) {
        this.receivedPublicKey = receivedPublicKey;
    }


    //~ ------------------------------------------------------------------------------------------------------------------

    /**
     * Génère une paire de clés (privée et publique) pour le côté courant.
     * KeyPairGenerator est une classe de la bibliothèque standard Java qui permet de générer des paires de clés.
     * On lui passe l'algorithme de chiffrement (ici Diffie-Hellman) et la taille de la clé (ici 2048 bits).
     * On génère ensuite la paire de clés avec la méthode generateKeyPair().
     * On récupère la clé privée et la clé publique avec les méthodes getPrivate() et getPublic().
     * On stocke ces clés dans les champs privateKey et publicKey.
     * On choisit une taille de clé de 2048 bits car c'est la taille recommandée par la NIST (National Institute of Standards and Technology).
     * On aurait pu choisir une taille de 1024 bits, mais c'est une taille qui est considérée comme faible aujourd'hui en termes de sécurité.
     */
    public void generateKeys() {
        try {
            // Initialisation du générateur de clés avec l'algorithme Diffie-Hellman et une taille de clé de 2048 bits
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(2048);
            // Génération de la paire de clés
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            // Attribution des clés privée et publique
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //~ ------------------------------------------------------------------------------------------------------------------

    /**
     * Renvoie la clé publique du côté courant.
     *
     * @return La clé publique du côté courant.
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    //~ ------------------------------------------------------------------------------------------------------------------

}