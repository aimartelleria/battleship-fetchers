package software.sebastian.mondragon.battleship;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void testMainSeEjecutaSinErrores() {
        Logger logger = Logger.getLogger(Main.class.getName());
        CapturingHandler handler = new CapturingHandler();
        boolean originalUseParentHandlers = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        try {
            assertDoesNotThrow(() -> Main.main(new String[0]));
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalUseParentHandlers);
            logger.setLevel(originalLevel);
        }
        List<String> logs = handler.snapshot();
        handler.close();
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Demo finalizado")));
    }

    private static final class CapturingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();
        private final SimpleFormatter formatter = new SimpleFormatter();

        private CapturingHandler() {
            setFormatter(formatter);
        }

        @Override
        public void publish(LogRecord recordBi) {
            if (recordBi != null) {
                messages.add(getFormatter().format(recordBi));
            }
        }

        @Override
        public void flush() {
            // nothing to flush
        }

        @Override
        public void close() {
            messages.clear();
        }

        boolean contains(String text) {
            return messages.stream().anyMatch(msg -> msg.contains(text));
        }

        List<String> snapshot() {
            return new ArrayList<>(messages);
        }
    }
}
