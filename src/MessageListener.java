
/*
    * Interface for the MessageListener class.
    * This interface is used to define the callback method onMessageReceived
    * The callback is defined with the method setMessageListener
 */
public interface MessageListener {
    void onMessageReceived(String message);
}
