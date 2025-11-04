package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test funcional con interacción real sobre MainMenuFrame.
 * Usa AssertJ Swing + SystemLambda (sin SecurityManager, compatible con Java 17+)
 */
public class MainMenuFrameTest {

    private FrameFixture window;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false"); // asegura modo gráfico

        MainMenuFrame frame = GuiActionRunner.execute(MainMenuFrame::new);
        window = new FrameFixture(frame);
        window.show(); // muestra el frame
    }

    @AfterEach
    void tearDown() {
        if (window != null)
            window.cleanUp();

        // cerrar cualquier otro frame abierto por los tests
        for (Frame f : Frame.getFrames()) {
            if (f.isVisible()) f.dispose();
        }
    }

    @Test
    @DisplayName("El frame se crea correctamente con los tres botones")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Menú Principal", frame.getTitle());
        assertTrue(frame.isVisible());
        window.button("crearBtn").requireVisible();
        window.button("unirseBtn").requireVisible();
        window.button("salirBtn").requireVisible();
    }

    @Test
    @DisplayName("Click en 'Crear partida' cierra el menú y abre GameHostFrame")
    void shouldOpenHostFrameOnClickCreate() {
        window.button("crearBtn").click();

        assertFalse(window.target().isVisible(), "El menú principal debe cerrarse");

        boolean opened = false;
        for (Frame f : Frame.getFrames()) {
            if (f instanceof GameHostFrame && f.isVisible()) {
                opened = true;
                f.dispose();
                break;
            }
        }
        assertTrue(opened, "GameHostFrame debería abrirse al pulsar Crear partida");
    }

    @Test
    @DisplayName("Click en 'Unirse a partida' cierra el menú y abre GameJoinFrame")
    void shouldOpenJoinFrameOnClickJoin() {
        window.button("unirseBtn").click();

        assertFalse(window.target().isVisible(), "El menú principal debe cerrarse");

        boolean opened = false;
        for (Frame f : Frame.getFrames()) {
            if (f instanceof GameJoinFrame && f.isVisible()) {
                opened = true;
                f.dispose();
                break;
            }
        }
        assertTrue(opened, "GameJoinFrame debería abrirse al pulsar Unirse a partida");
    }

    /*@Test   
    @DisplayName("Click en 'Salir' ejecuta onExit()")
    void shouldInvokeOnExitWhenClickExit() throws Exception {
        // bandera para verificar que se llamó a onExit()
        final boolean[] exitCalled = {false};

        // creamos una subclase que sobreescribe onExit()
        MainMenuFrame frame = GuiActionRunner.execute(() -> new MainMenuFrame() {
            @Override
            protected void onExit() {
                exitCalled[0] = true; // en lugar de System.exit(0)
            }
        });

        // mostramos ese frame y creamos una nueva fixture
        if (window != null) window.cleanUp();
        window = new FrameFixture(frame);
        window.show();

        // simulamos click real
        SwingUtilities.invokeAndWait(() -> window.button("salirBtn").click());

        assertTrue(exitCalled[0], "onExit() debería haberse llamado al pulsar 'Salir'");
    }*/
}
