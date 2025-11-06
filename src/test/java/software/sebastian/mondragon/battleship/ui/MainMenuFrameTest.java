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

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false"); // asegura modo gráfico
        sessionSupplier = StubClientSession::new;

        MainMenuFrame frame = GuiActionRunner.execute(() -> new MainMenuFrame(sessionSupplier));
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

    @Test
    @DisplayName("Click en 'Crear partida' cierra el menú y abre GameHostFrame")
    void shouldOpenHostFrameOnClickCreate() {
        window.button("crearBtn").click();

        assertFalse(window.target().isVisible(), "El menú principal debe cerrarse");

        boolean opened = false;
        for (Frame f : Frame.getFrames()) {
            if (f instanceof GameHostFrame hostFrame && hostFrame.isVisible()) {
                opened = true;
                hostFrame.dispose();
                break;
            }
        }
        assertTrue(opened, "GameHostFrame debería abrirse al pulsar Crear partida");
    }

    @Test
    @DisplayName("Click en 'Unirse a partida' cierra el menú y abre GameJoinFrame")
    void shouldOpenJoinFrameOnClickJoin() {
        window.button("unirseBtn").click();

        assertFalse(window.target().isVisible(), "El menú principal debe cerrarse");

        boolean opened = false;
        for (Frame f : Frame.getFrames()) {
            if (f instanceof GameJoinFrame joinFrame && joinFrame.isVisible()) {
                opened = true;
                joinFrame.dispose();
                break;
            }
        }
        assertTrue(opened, "GameJoinFrame debería abrirse al pulsar Unirse a partida");
    }

}

