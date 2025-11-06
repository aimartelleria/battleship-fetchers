package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;
import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class GameHostFrameTest {

    private FrameFixture window;
    private ClientSession sessionStub;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false");
        sessionStub = new StubClientSession();
        GameHostFrame frame = GuiActionRunner.execute(() -> new GameHostFrame(() -> sessionStub));
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
        window.label("statusLabel").requireVisible();
        window.textBox("infoArea").requireNotEditable();
        window.textBox("notificationArea").requireNotEditable();
        window.button("cancelButton").requireText("Cancelar / Volver");
    }

}
