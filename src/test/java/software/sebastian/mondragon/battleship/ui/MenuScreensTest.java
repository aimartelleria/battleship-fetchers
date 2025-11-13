package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;
import software.sebastian.mondragon.battleship.ui.support.SwingTestSupport;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class MenuScreensTest {

    private FrameFixture window;

    @AfterEach
    void tearDown() {
        SwingTestSupport.cleanup(window);
        window = null;
    }

    @Test
    @DisplayName("El menú principal muestra los tres botones básicos")
    void mainMenuDisplaysButtons() {
        window = SwingTestSupport.showFrame(() -> new MainMenuFrame(StubClientSession::new));

        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Menú Principal", frame.getTitle());
        window.button("crearBtn").requireVisible();
        window.button("unirseBtn").requireVisible();
        window.button("salirBtn").requireVisible();
    }

    @Test
    @DisplayName("La pantalla de host expone controles y textos esperados")
    void hostScreenShowsControls() {
        StubClientSession sessionStub = new StubClientSession();
        window = SwingTestSupport.showFrame(() -> new GameHostFrame(() -> sessionStub));

        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Crear Partida", frame.getTitle());
        window.label("statusLabel").requireVisible();
        window.textBox("infoArea").requireNotEditable();
        window.textBox("notificationArea").requireNotEditable();
        window.button("cancelButton").requireText("Cancelar / Volver");
    }
}
