import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Test {
    private static final Logger logger = Logger.getLogger("MyApp");
    public static void main(String[] args) {
        logger.info("This logs an INFO level message");
        logger.warning("This logs a WARNING level message");
        logger.severe("This logs a SEVERE level message");
    }
}
