package software.sebastian.mondragon.battleship;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void testMainSeEjecutaSinErrores() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(buffer)) {
            System.setOut(ps);
            assertDoesNotThrow(() -> Main.main(new String[0]));
        } finally {
            System.setOut(original);
        }
        assertTrue(buffer.toString().contains("Demo finalizado"));
    }
}
