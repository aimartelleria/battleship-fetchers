package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;
import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class GameJoinFrameTest {

    private FrameFixture window;
    private ClientSession sessionStub;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false");
        sessionStub = new StubClientSession();
        GameJoinFrame frame = GuiActionRunner.execute(() -> new GameJoinFrame(() -> sessionStub));
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
        window.label("statusLabel").requireVisible();
        window.textBox("gameCodeField").requireVisible();
        window.button("connectButton").requireText("Conectar");
    }
}
