package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameHostFrameTest {

    private FrameFixture window;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false");
        GameHostFrame frame = GuiActionRunner.execute(GameHostFrame::new);
        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    void tearDown() {
        if (window != null) window.cleanUp();
        for (Frame f : Frame.getFrames()) if (f.isVisible()) f.dispose();
    }

    @Test
    @DisplayName("Inicializa la interfaz correctamente")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Crear Partida", frame.getTitle());
        window.label().requireText("Esperando jugador...");
        window.textBox().requireNotEditable();
        window.button().requireText("Cancelar / Volver");
    }

    /*@Test
    @DisplayName("Click en Cancelar/Volver abre MainMenuFrame")
    void shouldReturnToMainMenuOnCancel() throws Exception {
        SwingUtilities.invokeAndWait(() -> window.button().click());

        boolean opened = false;
        for (Frame f : Frame.getFrames()) {
            if (f instanceof MainMenuFrame && f.isVisible()) {
                opened = true;
                f.dispose();
                break;
            }
        }
        assertTrue(opened, "Debería abrirse MainMenuFrame al pulsar Cancelar / Volver");
    }*/

    /*@Test
    @DisplayName("Simula que el Timer crea un GameBoardFrame tras 3 segundos")
    void shouldOpenGameBoardFrameAfterTimer() throws Exception {
        // Espera algo más de 3 segundos para que se dispare el Timer
        Thread.sleep(3500);

        boolean opened = false;
        for (Frame f : Frame.getFrames()) {
            if (f instanceof GameBoardFrame && f.isVisible()) {
                opened = true;
                f.dispose();
                break;
            }
        }
        assertTrue(opened, "GameBoardFrame debería abrirse tras la conexión simulada");
    }*/
}
