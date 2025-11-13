package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;
import software.sebastian.mondragon.battleship.ui.support.SwingTestSupport;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.*;

class GameBoardFrameTest {

    private FrameFixture window;
    private JButton[][] ownGrid;
    private JButton[][] enemyGrid;
    private JLabel statusLabel;

    @BeforeEach
    void setUp() {
        StubClientSession sessionStub = new StubClientSession();
        window = SwingTestSupport.showFrame(() -> new GameBoardFrame(sessionStub, StubClientSession::new));
        GameBoardFrame frame = (GameBoardFrame) window.target();
        ownGrid = extractGrid(frame, "ownGrid");
        enemyGrid = extractGrid(frame, "enemyGrid");
        statusLabel = extractField(frame, "statusLabel", JLabel.class);
    }

    @AfterEach
    void tearDown() {
        SwingTestSupport.cleanup(window);
    }

    @Test
    @DisplayName("Inicializa UI principal")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Tablero de Juego", frame.getTitle());

        window.button(JButtonMatcher.withText("Listo ✔")).requireVisible();
        window.button(JButtonMatcher.withText("Dirección: Horizontal")).requireVisible();
        assertNotNull(ownGrid);
        assertNotNull(enemyGrid);
        assertNotNull(statusLabel);
    }

    @Test
    @DisplayName("Rotar dirección Horizontal/Vertical")
    void shouldToggleDirection() throws Exception {
        JButton rotateBtn = window.button(JButtonMatcher.withText("Dirección: Horizontal")).target();
        SwingUtilities.invokeAndWait(rotateBtn::doClick);
        assertTrue(rotateBtn.getText().contains("Vertical"));
        SwingUtilities.invokeAndWait(rotateBtn::doClick);
        assertTrue(rotateBtn.getText().contains("Horizontal"));
    }

    @Test
    @DisplayName("Botón Listo desactiva y actualiza estado")
    void shouldSetReadyStatus() throws Exception {
        JButton readyBtn = window.button(JButtonMatcher.withText("Listo ✔")).target();
        SwingUtilities.invokeAndWait(readyBtn::doClick);
        assertFalse(readyBtn.isEnabled());
        assertTrue(statusLabel.getText().contains("Listo"));
    }

    @Test
    @DisplayName("Coloca barco tamaño 2 en (0,0)")
    void shouldPlaceShipOnOwnBoard() {
        JPanel shipPanel = selectShip("ship-2");
        releaseShipOnCell(0, 0);

        JButton targetCell = ownGrid[0][0];
        assertFalse(targetCell.isEnabled());
        assertEquals(Color.GRAY, targetCell.getBackground());
        assertFalse(shipPanel.isEnabled());
    }

    @Test
    @DisplayName("Rechaza fuera de límites (horizontal)")
    void shouldRejectShipPlacementOutsideBoundsHorizontal() {
        JPanel shipPanel = selectShip("ship-5");
        releaseShipOnCell(0, 7);
        requireMessageAndDismiss("El barco no cabe horizontalmente aquí.");
        assertTrue(ownGrid[0][7].isEnabled());
        assertTrue(shipPanel.isEnabled());
    }

    @Test
    @DisplayName("Rechaza fuera de límites (vertical)")
    void shouldRejectShipPlacementOutsideBoundsVertical() {
        JButton rotateBtn = window.button(JButtonMatcher.withText("Dirección: Horizontal")).target();
        SwingUtilities.invokeLater(rotateBtn::doClick);
        window.robot().waitForIdle();

        JPanel shipPanel = selectShip("ship-5");
        releaseShipOnCell(7, 0);
        requireMessageAndDismiss("El barco no cabe verticalmente aquí.");
        assertTrue(ownGrid[7][0].isEnabled());
        assertTrue(shipPanel.isEnabled());
    }

    @Test
    @DisplayName("Rechaza solapamiento con barco existente")
    void shouldRejectShipOverlap() {
        JPanel first = selectShip("ship-2");
        releaseShipOnCell(0, 0);
        assertFalse(first.isEnabled());

        JPanel second = selectShip("ship-3");
        releaseShipOnCell(0, 0);
        requireMessageAndDismiss("Ya hay un barco en esa posición.");
        assertTrue(second.isEnabled());
        assertFalse(ownGrid[0][0].isEnabled());
    }

    @Test
    @DisplayName("Ignora mouseReleased sin barco seleccionado")
    void ignoresMouseReleaseWithoutSelectedShip() {
        releaseShipOnCell(0, 0);
        assertTrue(ownGrid[0][0].isEnabled());
    }

    @Test
    @DisplayName("mouseReleased en el panel del barco limpia el borde")
    void shipMouseReleasedClearsBorder() {
        JPanel ship = selectShip("ship-2");
        dispatchMouseReleased(ship);
        assertNull(ship.getBorder());
    }

    @Test
    @DisplayName("Cobertura de helpers de límites")
    void boundsHelpersCovered() throws Exception {
        GameBoardFrame frame = (GameBoardFrame) window.target();
        setPrivateField(frame, "selectedShipSize", 3);

        boolean horizontalOut = (boolean) invokePrivate(frame, "isPlacementWithinBounds",
                new Class[]{int.class, int.class, int.class, int.class}, 8, 8, 0, 1);
        assertFalse(horizontalOut);

        boolean horizontalIn = (boolean) invokePrivate(frame, "isPlacementWithinBounds",
                new Class[]{int.class, int.class, int.class, int.class}, 0, 0, 0, 1);
        assertTrue(horizontalIn);

        boolean insideNegative = (boolean) invokePrivate(frame, "isInsideGrid",
                new Class[]{int.class, int.class}, -1, 0);
        assertFalse(insideNegative);

        boolean insideEdge = (boolean) invokePrivate(frame, "isInsideGrid",
                new Class[]{int.class, int.class}, 9, 9);
        assertTrue(insideEdge);
    }

    private JPanel selectShip(String shipName) {
        JPanel panel = findByName((Container) window.target(), shipName);
        assertNotNull(panel, "No se encontró el barco " + shipName);
        dispatchMousePressed(panel);
        return panel;
    }

    private void releaseShipOnCell(int row, int col) {
        JButton cell = ownGrid[row][col];
        assertNotNull(cell, "No se encontró celda " + row + "," + col);
        dispatchMouseReleased(cell);
    }

    private void requireMessageAndDismiss(String message) {
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane()
                .withTimeout(2000)
                .using(window.robot());
        optionPane.requireMessage(message);
        optionPane.okButton().click();
        window.robot().waitForIdle();
    }

    private void dispatchMousePressed(Component component) {
        SwingUtilities.invokeLater(() -> component.dispatchEvent(new MouseEvent(
                component, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, 10, 10, 1, false
        )));
        window.robot().waitForIdle();
    }

    private void dispatchMouseReleased(Component component) {
        SwingUtilities.invokeLater(() -> component.dispatchEvent(new MouseEvent(
                component, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, 10, 10, 1, false
        )));
        window.robot().waitForIdle();
    }

    private JButton[][] extractGrid(GameBoardFrame frame, String fieldName) {
        try {
            var field = GameBoardFrame.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (JButton[][]) field.get(frame);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo acceder a " + fieldName, ex);
        }
    }

    private <T> T extractField(GameBoardFrame frame, String fieldName, Class<T> type) {
        try {
            var field = GameBoardFrame.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(frame));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo acceder a " + fieldName, ex);
        }
    }

    private JPanel findByName(Container root, String name) {
        if (name.equals(root.getName()) && root instanceof JPanel panel) {
            return panel;
        }
        for (Component component : root.getComponents()) {
            if (component instanceof Container container) {
                JPanel found = findByName(container, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object invokePrivate(Object target, String method, Class<?>[] parameterTypes, Object... args) throws Exception {
        var m = target.getClass().getDeclaredMethod(method, parameterTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }
}
