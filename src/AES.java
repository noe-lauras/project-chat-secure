import javax.crypto.* ;
import java.security.* ;
import java.util.Arrays;

/*
La classe AES permet de chiffrer et déchiffrer des messages.
Elle a été initialement définie lorsque nous n'utilisions pas Diffie-Hellman.
Elle n'est plus utilisée dans la version finale du projet.
 */
class AES {
    KeyGenerator kg;
    Key key;
    Cipher cipher;
    AES() throws NoSuchPaddingException, NoSuchAlgorithmException {
        cipher=Cipher.getInstance("AES");
        kg=KeyGenerator.getInstance("AES");
    }
    void genereKey()throws NoSuchAlgorithmException {
        key= kg.generateKey();
    }

    byte[] encrypt(String message){
        byte[] result = new byte[2];
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] data = message.getBytes();
            result = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    String decrypt(byte[] result){
        String original="";
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] tempo=cipher.doFinal(result);
            original=new String(tempo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(original);
    }

    String toString1(Key result) {
        String res = "";
        for (byte b : result.getEncoded()) {
            res += Byte.toString(b);
        }
        return res;
    }
    Key getSecretKey() {
        return key;
    }
}