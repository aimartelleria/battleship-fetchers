package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.*;
import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;
import software.sebastian.mondragon.battleship.ui.support.SwingTestSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class GameBoardFrameTest {

    private FrameFixture window;
    private JButton[][] ownGrid;
    private ClientSession sessionStub;

    @BeforeEach
    void setUp() {
        sessionStub = new StubClientSession();
        window = SwingTestSupport.showFrame(() -> new GameBoardFrame(sessionStub, StubClientSession::new));
        ownGrid = extractOwnGrid((GameBoardFrame) window.target());
    }

    @AfterEach
    void tearDown() {
        SwingTestSupport.cleanup(window);
    }

    @Test
    @DisplayName("Inicializa correctamente los elementos principales")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Tablero de Juego", frame.getTitle());
        window.button("readyButton").requireVisible();
        window.button("rotateButton").requireVisible();
        window.button("exitButton").requireVisible();
        window.label("statusLabel").requireVisible();
    }

    @Test
    @DisplayName("Al pulsar el botón de dirección cambia entre Horizontal y Vertical")
    void shouldToggleDirection() throws Exception {
        JButton rotateBtn = window.button("rotateButton").target();
        SwingUtilities.invokeAndWait(rotateBtn::doClick);
        assertTrue(rotateBtn.getText().contains("Vertical"), "Debe cambiar a Vertical");

        SwingUtilities.invokeAndWait(rotateBtn::doClick);
        assertTrue(rotateBtn.getText().contains("Horizontal"), "Debe volver a Horizontal");
    }

    @Test
    @DisplayName("Al pulsar el botón Listo cambia el estado y se desactiva")
    void shouldSetReadyStatus() throws Exception {
        JButton readyBtn = window.button("readyButton").target();
        JLabel statusLabel = window.label("statusLabel").target();

        assertNotNull(readyBtn);
        assertNotNull(statusLabel);

        SwingUtilities.invokeAndWait(readyBtn::doClick);

        assertFalse(readyBtn.isEnabled(), "El botón Listo debe desactivarse");
        assertTrue(statusLabel.getText().contains("Listo"), "El estado debe actualizarse");
    }

    @Test
    @DisplayName("Simula colocar un barco en el tablero propio")
    void shouldPlaceShipOnOwnBoard() {
        JPanel shipPanel = selectShip("ship-2");
        releaseShipOnCell(0, 0);

        JButton targetCell = ownGrid[0][0];
        assertFalse(targetCell.isEnabled(), "La celda debería estar desactivada tras colocar el barco");
        assertEquals(Color.GRAY, targetCell.getBackground());
        assertFalse(shipPanel.isEnabled(), "El barco debería desactivarse tras colocarlo");
    }

    @Test
    @DisplayName("Evita colocar un barco fuera del tablero y muestra un mensaje")
    void shouldRejectShipPlacementOutsideBounds() {
        JPanel shipPanel = selectShip("ship-5");
        releaseShipOnCell(0, 7);

        requireMessageAndDismiss("El barco no cabe horizontalmente aquí.");
        assertTrue(ownGrid[0][7].isEnabled(), "La celda inicial no debe desactivarse");
        assertTrue(shipPanel.isEnabled(), "El barco debe seguir disponible tras el rechazo");
    }

    @Test
    @DisplayName("Impide colocar un barco sobre otro existente")
    void shouldRejectShipOverlap() {
        JPanel firstShip = selectShip("ship-2");
        releaseShipOnCell(0, 0);
        assertFalse(firstShip.isEnabled(), "El primer barco debe quedar desactivado");

        JPanel secondShip = selectShip("ship-3");
        releaseShipOnCell(0, 0);

        requireMessageAndDismiss("Ya hay un barco en esa posición.");
        assertTrue(secondShip.isEnabled(), "El segundo barco debe permanecer disponible");
        assertFalse(ownGrid[0][0].isEnabled(), "La celda original debe seguir ocupada");
    }

    @Test
    @DisplayName("Permite colocar un barco vertical cuando se rota la dirección")
    void shouldPlaceShipVerticallyAfterRotation() {
        JButton rotateBtn = window.button("rotateButton").target();
        SwingUtilities.invokeLater(rotateBtn::doClick);
        window.robot().waitForIdle();

        JPanel shipPanel = selectShip("ship-3");
        releaseShipOnCell(0, 0);

        assertFalse(shipPanel.isEnabled(), "El barco debe desactivarse tras colocarse");
        assertFalse(ownGrid[0][0].isEnabled());
        assertFalse(ownGrid[1][0].isEnabled());
        assertFalse(ownGrid[2][0].isEnabled());
        assertEquals(Color.GRAY, ownGrid[0][0].getBackground());
        assertEquals(Color.GRAY, ownGrid[1][0].getBackground());
        assertEquals(Color.GRAY, ownGrid[2][0].getBackground());
    }

    private JPanel selectShip(String shipName) {
        JPanel shipPanel = (JPanel) TestUtils.findComponentByName(window.target(), shipName);
        assertNotNull(shipPanel, "No se encontró el barco " + shipName);

        SwingUtilities.invokeLater(() -> shipPanel.dispatchEvent(new MouseEvent(
                shipPanel,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                10,
                10,
                1,
                false
        )));
        window.robot().waitForIdle();
        return shipPanel;
    }

    private void releaseShipOnCell(int row, int col) {
        JButton targetCell = ownGrid[row][col];
        assertNotNull(targetCell, "No se encontró la celda en " + row + "," + col);

        SwingUtilities.invokeLater(() -> targetCell.dispatchEvent(new MouseEvent(
                targetCell,
                MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                0,
                5,
                5,
                1,
                false
        )));
        window.robot().waitForIdle();
    }

    private void requireMessageAndDismiss(String message) {
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane()
                .withTimeout(2000)
                .using(window.robot());
        optionPane.requireMessage(message);
        optionPane.okButton().click();
        window.robot().waitForIdle();
    }

    private JButton[][] extractOwnGrid(GameBoardFrame frame) {
        try {
            Field ownGridField = GameBoardFrame.class.getDeclaredField("ownGrid");
            ownGridField.setAccessible(true);
            return (JButton[][]) ownGridField.get(frame);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("No se pudo acceder a la cuadrícula propia", e);
        }
    }
}
