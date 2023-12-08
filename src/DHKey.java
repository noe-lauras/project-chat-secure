import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;

public class DHKey {

    //~ --- [CHAMPS D'INSTANCE] ---------------------------------------------------------------------------------------

    private PrivateKey privateKey;  // Clé privée du côté courant
    private PublicKey publicKey;    // Clé publique du côté courant
    private PublicKey receivedPublicKey;  // Clé publique reçue de l'autre côté
    private byte[] secretKey;       // Clé secrète partagée
    private String secretMessage;    // Message secret après déchiffrement

    //~ --- [MÉTHODES] --------------------------------------------------------------------------------------------------

    /**
     * Chiffre le message avec la clé secrète partagée et envoie le message à l'autre côté pour déchiffrement.
     *
     * @param msg Message à chiffrer et envoyer.
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

    //~ ------------------------------------------------------------------------------------------------------------------

    /**
     * Génère la clé secrète commune à partir de la clé privée et de la clé publique reçue de l'autre côté.
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

    // Fonction auxiliaire pour ajuster la longueur de la clé
    private byte[] adjustKeyLength(byte[] key, int desiredLength) {
        if (key.length == desiredLength) {
            return key;
        } else if (key.length < desiredLength) {
            // Si la clé est trop courte, ajoutez du padding (ou faites d'autres ajustements selon vos besoins)
            return Arrays.copyOf(key, desiredLength);
        } else {
            // Si la clé est trop longue, tronquez-la (ou faites d'autres ajustements selon vos besoins)
            return Arrays.copyOfRange(key, 0, desiredLength);
        }
    }

    //~ ------------------------------------------------------------------------------------------------------------------

    /**
     * Reçoit la clé publique de l'autre côté de la communication.
     *
     * @param dh Instance de DHKey représentant l'autre côté de la communication.
     */
    public void receivePublicKeyFrom(final DHKey dh) {
        receivedPublicKey = dh.getPublicKey();
    }

    //~ ------------------------------------------------------------------------------------------------------------------

    /**
     * Affiche le message secret déchiffré.
     */
    public void whisperTheSecretMessage() {
        System.out.println(secretMessage);
    }

    //~ ------------------------------------------------------------------------------------------------------------------

    /**
     * Réduit la taille de la clé secrète générée. Dans cet exemple, on utilise les premiers 8 octets de la clé générée.
     *
     * @param longKey Clé secrète générée.
     * @return Clé secrète réduite.
     */
    private byte[] shortenSecretKey(final byte[] longKey) {
        try {
            // Utilisation des premiers 8 octets de la clé générée
            final byte[] shortenedKey = new byte[8];
            System.arraycopy(longKey, 0, shortenedKey, 0, shortenedKey.length);
            return shortenedKey;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}