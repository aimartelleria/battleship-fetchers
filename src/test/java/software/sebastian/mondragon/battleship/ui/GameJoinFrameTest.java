package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class GameJoinFrameTest {

    private FrameFixture window;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false");
        GameJoinFrame frame = GuiActionRunner.execute(GameJoinFrame::new);
        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    void tearDown() {
        if (window != null) window.cleanUp();
        for (Frame f : Frame.getFrames()) if (f.isVisible()) f.dispose();
    }

    @Test
    @DisplayName("Inicializa correctamente los controles")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Unirse a Partida", frame.getTitle());
        window.textBox().requireVisible();
        window.button().requireText("Conectar");
    }
}
