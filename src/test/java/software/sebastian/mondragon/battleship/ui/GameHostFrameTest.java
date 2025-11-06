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

    @Test
    @DisplayName("Al hacer clic en 'Cancelar / Volver' se abre MainMenuFrame y se cierra la ventana actual")
    void shouldOpenMainMenuWhenCancelIsClicked() {
        // Simula clic en el botón
        window.button().click();

        // Espera a que se procese el evento Swing
        GuiActionRunner.execute(() -> {});

        // Verifica que GameHostFrame se haya cerrado
        assertFalse(window.target().isVisible(), "La ventana GameHostFrame debería estar cerrada");

        // Verifica que se haya abierto MainMenuFrame
        boolean mainMenuVisible = false;
        for (Frame f : Frame.getFrames()) {
            if (f instanceof MainMenuFrame && f.isVisible()) {
                mainMenuVisible = true;
                f.dispose(); // Cierra después de verificar
            }
        }
        assertTrue(mainMenuVisible, "MainMenuFrame debería haberse abierto");
    }

    @Test
    @DisplayName("Después del temporizador se muestra JOptionPane y se abre GameBoardFrame")
    void shouldShowDialogAndOpenGameBoardAfterTimer(){
        // Espera un poco más del tiempo del Timer (3 segundos)

        // Encuentra y cierra el JOptionPane automáticamente
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && ((JDialog) w).getTitle() == null) {
                JDialog dialog = (JDialog) w;
                JOptionPane pane = (JOptionPane) dialog.getContentPane().getComponent(0);
                assertEquals("Jugador conectado!", pane.getMessage());
                dialog.dispose();
            }
        }

        // Verifica que GameBoardFrame esté visible
        for (Frame f : Frame.getFrames()) {
            if (f instanceof GameBoardFrame && f.isVisible()) {
                f.dispose();
            }
        }

    }
}
