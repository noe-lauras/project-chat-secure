import javax.crypto.* ;
import java.security.* ;
import java.util.Arrays;

class AES {
    KeyGenerator kg;
    Key key;
    Cipher cipher;
    AES() throws NoSuchAlgorithmException, NoSuchPaddingException {
         kg=KeyGenerator.getInstance("AES");
         key= kg.generateKey();
         cipher=Cipher.getInstance("AES");

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
}