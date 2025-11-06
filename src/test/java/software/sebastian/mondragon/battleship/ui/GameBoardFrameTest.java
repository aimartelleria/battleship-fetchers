package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.*;

import software.sebastian.mondragon.battleship.ui.support.StubClientSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import static org.junit.jupiter.api.Assertions.*;

class GameBoardFrameMinimalTest {

    private FrameFixture window;
    private JButton[][] ownGrid;
    private JButton[][] enemyGrid;
    private JLabel statusLabel;

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Entorno headless: se omiten pruebas de interfaz Swing");
        System.setProperty("java.awt.headless", "false");
        StubClientSession sessionStub = new StubClientSession();
        GameBoardFrame frame = GuiActionRunner.execute(() -> new GameBoardFrame(sessionStub, StubClientSession::new));
        window = new FrameFixture(frame);
        window.show();

        ownGrid = extractGrid(frame, "ownGrid");
        enemyGrid = extractGrid(frame, "enemyGrid");
        statusLabel = extractField(frame, "statusLabel", JLabel.class);
    }

    @AfterEach
    void tearDown() {
        if (window != null) window.cleanUp();
        for (Frame f : Frame.getFrames()) if (f.isVisible()) f.dispose();
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
        JButton cell = ownGrid[0][0];
        assertFalse(cell.isEnabled());
        assertEquals(Color.GRAY, cell.getBackground());
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
    @DisplayName("Clic en tablero enemigo pinta azul")
    void enemyGridClickPaintsBlue() {
        JButton cell = enemyGrid[1][1];
        SwingUtilities.invokeLater(cell::doClick);
        window.robot().waitForIdle();
        assertEquals(Color.BLUE, cell.getBackground());
    }

    // ===== Helpers mínimos =====

    private JPanel selectShip(String name) {
        JPanel ship = findByName((Container) window.target(), name);
        assertNotNull(ship, "No se encontró " + name);
        dispatchMousePressed(ship);
        return ship;
    }

    private void releaseShipOnCell(int r, int c) {
        JButton cell = ownGrid[r][c];
        assertNotNull(cell, "Celda " + r + "," + c + " inexistente");
        dispatchMouseReleased(cell);
    }

    private void requireMessageAndDismiss(String msg) {
        JOptionPaneFixture pane = JOptionPaneFinder.findOptionPane()
                .withTimeout(2000)
                .using(window.robot());
        pane.requireMessage(msg);
        pane.okButton().click();
        window.robot().waitForIdle();
    }

    private void dispatchMousePressed(Component c) {
        SwingUtilities.invokeLater(() -> c.dispatchEvent(new MouseEvent(
                c, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, 10, 10, 1, false
        )));
        window.robot().waitForIdle();
    }

    private void dispatchMouseReleased(Component c) {
        SwingUtilities.invokeLater(() -> c.dispatchEvent(new MouseEvent(
                c, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, 10, 10, 1, false
        )));
        window.robot().waitForIdle();
    }

    @SuppressWarnings("unchecked")
    private JButton[][] extractGrid(GameBoardFrame frame, String field) {
        try {
            var f = GameBoardFrame.class.getDeclaredField(field);
            f.setAccessible(true);
            return (JButton[][]) f.get(frame);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo acceder a " + field, e);
        }
    }

    private <T> T extractField(GameBoardFrame frame, String field, Class<T> type) {
        try {
            var f = GameBoardFrame.class.getDeclaredField(field);
            f.setAccessible(true);
            return type.cast(f.get(frame));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo acceder a " + field, e);
        }
    }

    private JPanel findByName(Container root, String name) {
        if (name.equals(root.getName())) return (JPanel) root;
        for (Component c : root.getComponents()) {
            if (c instanceof Container) {
                JPanel found = findByName((Container) c, name);
                if (found != null) return found;
            }
        }
        return null;
    }
    

    // --- NUEVOS TESTS PARA 100% COVERAGE ---

@Test
@DisplayName("Ignora mouseReleased sin barco seleccionado (condición evaluada)")
void ignoresMouseReleaseWithoutSelectedShip() {
    // No llamamos a selectShip: selectedShip == null
    releaseShipOnCell(0, 0); // dispara mouseReleased de la celda
    assertTrue(ownGrid[0][0].isEnabled());
}

@Test
@DisplayName("mouseReleased en el panel de barco limpia el borde")
void shipMouseReleasedClearsBorder() {
    JPanel ship = selectShip("ship-2"); // mousePressed -> pone borde rojo
    dispatchMouseReleased(ship);         // mouseReleased del propio ship
    assertNull(ship.getBorder());
}

@Test
@DisplayName("placeShip retorna si selectedShip es null")
void placeShipReturnsWhenNoSelectedShip() throws Exception {
    GameBoardFrame frame = (GameBoardFrame) window.target();
    setPrivateField(frame, "selectedShip", null);
    setPrivateField(frame, "selectedShipSize", 0);
    invokePrivateVoid(frame, "placeShip", new Class[]{int.class, int.class}, 0, 0);
    assertTrue(ownGrid[0][0].isEnabled());
}

@Test
@DisplayName("Cobertura de isPlacementWithinBounds e isInsideGrid")
void boundsHelpersCovered() throws Exception {
    GameBoardFrame frame = (GameBoardFrame) window.target();
    // El método usa selectedShipSize internamente
    setPrivateField(frame, "selectedShipSize", 3);

    // Caso fuera de límites (horizontal desde 8,8 con tamaño 3)
    boolean out = (boolean) invokePrivate(frame, "isPlacementWithinBounds",
            new Class[]{int.class, int.class, int.class, int.class}, 8, 8, 0, 1);
    assertFalse(out);

    // Caso válido (horizontal desde 0,0 con tamaño 3)
    boolean in = (boolean) invokePrivate(frame, "isPlacementWithinBounds",
            new Class[]{int.class, int.class, int.class, int.class}, 0, 0, 0, 1);
    assertTrue(in);

    // Cobertura de isInsideGrid
    boolean insideNeg = (boolean) invokePrivate(frame, "isInsideGrid",
            new Class[]{int.class, int.class}, -1, 0);
    assertFalse(insideNeg);

    boolean insideEdge = (boolean) invokePrivate(frame, "isInsideGrid",
            new Class[]{int.class, int.class}, 9, 9);
    assertTrue(insideEdge);
}

// --- HELPERS REFLECTION (añádelos al final de la clase de test) ---

private void setPrivateField(Object target, String field, Object value) throws Exception {
    var f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, value);
}

private Object invokePrivate(Object target, String method, Class<?>[] types, Object... args) throws Exception {
    var m = target.getClass().getDeclaredMethod(method, types);
    m.setAccessible(true);
    return m.invoke(target, args);
}

private void invokePrivateVoid(Object target, String method, Class<?>[] types, Object... args) throws Exception {
    invokePrivate(target, method, types, args);
}

}
