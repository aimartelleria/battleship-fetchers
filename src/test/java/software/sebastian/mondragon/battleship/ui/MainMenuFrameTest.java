package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;
import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuFrameTest {

    private FrameFixture window;
    private Supplier<ClientSession> sessionSupplier;
    private MainMenuFrame frame;

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Entorno headless: se omiten pruebas de interfaz Swing");
        System.setProperty("java.awt.headless", "false"); // asegura modo gráfico
        sessionSupplier = StubClientSession::new;

        frame = GuiActionRunner.execute(() -> new MainMenuFrame(sessionSupplier));
        window = new FrameFixture(frame);
        window.show(); // muestra el frame
    }

    @AfterEach
    void tearDown() {
        if (window != null)
            window.cleanUp();

        // cerrar cualquier otro frame abierto por los tests
        for (Frame f : Frame.getFrames()) {
            if (f.isVisible()) f.dispose();
        }

        frame = null;
        window = null;
    }

    @Test
    @DisplayName("El frame se crea correctamente con los tres botones")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Menú Principal", frame.getTitle());
        assertTrue(frame.isVisible());
        window.button("crearBtn").requireVisible();
        window.button("unirseBtn").requireVisible();
        window.button("salirBtn").requireVisible();
    }
}
