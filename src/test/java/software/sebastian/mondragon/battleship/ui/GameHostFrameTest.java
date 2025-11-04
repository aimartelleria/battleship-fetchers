package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class GameHostFrameTest {

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

}
