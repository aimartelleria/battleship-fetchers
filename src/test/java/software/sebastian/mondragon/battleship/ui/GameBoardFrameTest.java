package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameBoardFrameTest {

    private FrameFixture window;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false");
        GameBoardFrame frame = GuiActionRunner.execute(() -> new GameBoardFrame(true));
        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    void tearDown() {
        if (window != null) window.cleanUp();
        for (Frame f : Frame.getFrames()) if (f.isVisible()) f.dispose();
    }

    @Test
    @DisplayName("Inicializa correctamente los elementos principales")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Tablero de Juego", frame.getTitle());
        window.button(JButtonMatcher.withText("Listo ✔")).requireVisible();
        window.button(JButtonMatcher.withText("Dirección: Horizontal")).requireVisible();
    }

    @Test
    @DisplayName("Al pulsar el botón de dirección cambia entre Horizontal y Vertical")
    void shouldToggleDirection() throws Exception {
        JButton rotateBtn = window.button(JButtonMatcher.withText("Dirección: Horizontal")).target();
        assertNotNull(rotateBtn);

        SwingUtilities.invokeAndWait(() -> rotateBtn.doClick());
        assertTrue(rotateBtn.getText().contains("Vertical"), "Debe cambiar a Vertical");

        SwingUtilities.invokeAndWait(() -> rotateBtn.doClick());
        assertTrue(rotateBtn.getText().contains("Horizontal"), "Debe volver a Horizontal");
    }

    @Test
    @DisplayName("Al pulsar el botón Listo cambia el estado y se desactiva")
    void shouldSetReadyStatus() throws Exception {
        JButton readyBtn = window.button(JButtonMatcher.withText("Listo ✔")).target();
        JLabel statusLabel = (JLabel) TestUtils.findComponentByText(window.target(), "Estado: Esperando al otro jugador...");

        assertNotNull(readyBtn);
        assertNotNull(statusLabel);

        SwingUtilities.invokeAndWait(() -> readyBtn.doClick());

        assertFalse(readyBtn.isEnabled(), "El botón Listo debe desactivarse");
        assertTrue(statusLabel.getText().contains("Listo"), "El estado debe actualizarse");
    }

    @Test
    @DisplayName("Simula colocar un barco en el tablero propio")
    void shouldPlaceShipOnOwnBoard() throws Exception {
        // accedemos a un barco del panel de barcos
        JPanel shipPanel = (JPanel) TestUtils.findComponentByName(window.target(), "ship-2");
        assertNotNull(shipPanel);

        // Simula seleccionar el barco
        SwingUtilities.invokeAndWait(() -> {
            shipPanel.dispatchEvent(new java.awt.event.MouseEvent(
                    shipPanel,
                    java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    0, 10, 10, 1, false
            ));
        });

        // Simula soltar el barco en una celda válida (0,0)
        JButton targetCell = (JButton) TestUtils.findButtonByPosition(window.target(), 0, 0);
        assertNotNull(targetCell);

        SwingUtilities.invokeAndWait(() -> targetCell.dispatchEvent(new java.awt.event.MouseEvent(
                targetCell,
                java.awt.event.MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                0, 5, 5, 1, false
        )));

        // Verificamos que la celda se haya desactivado (barco colocado)
        assertFalse(targetCell.isEnabled(), "La celda debería estar desactivada tras colocar el barco");
        assertEquals(Color.GRAY, targetCell.getBackground());
    }
}
