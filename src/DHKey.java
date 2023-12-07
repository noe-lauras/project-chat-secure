import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;

public class DHKey {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private PublicKey receivedPublicKey;
    private byte[] secretKey;
    private String secretMessage;

    public void generateKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(1024);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateCommonSecretKey() {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(receivedPublicKey, true);

            secretKey = shortenSecretKey(keyAgreement.generateSecret());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receivePublicKeyFrom(DHKey otherPerson) {
        receivedPublicKey = otherPerson.getPublicKey();
    }

    public void encryptAndSendMessage(String message, DHKey otherPerson) {
        try {
            SecretKey aesSecretKey = generateAESSecretKey();
            byte[] encryptedMessage = encryptWithAES(message, aesSecretKey);
            otherPerson.receiveAndDecryptMessage(encryptedMessage, aesSecretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receiveAndDecryptMessage(byte[] message, SecretKey aesSecretKey) {
        try {
            String decryptedMessage = decryptWithAES(message, aesSecretKey);
            System.out.println("Message reçu : " + decryptedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SecretKey generateAESSecretKey() {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(receivedPublicKey, true);
            byte[] dhSecret = keyAgreement.generateSecret();

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] aesKeyBytes = Arrays.copyOf(sha256.digest(dhSecret), 16); // 16 bytes = 128 bits

            return new SecretKeySpec(aesKeyBytes, "AES");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] encryptWithAES(String message, SecretKey aesSecretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesSecretKey);
            return cipher.doFinal(message.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String decryptWithAES(byte[] encryptedMessage, SecretKey aesSecretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesSecretKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedMessage);
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] shortenSecretKey(byte[] longKey) {
        try {
            // j'utilise 16 octets = 128 bits pour AES
            byte[] shortenedKey = new byte[16];
            System.arraycopy(longKey, 0, shortenedKey, 0, shortenedKey.length);
            return shortenedKey;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
