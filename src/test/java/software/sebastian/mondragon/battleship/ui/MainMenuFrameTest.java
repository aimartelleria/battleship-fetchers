package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuFrameTest {

    private FrameFixture window;
    private Supplier<ClientSession> sessionSupplier;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false"); // asegura modo gráfico
        sessionSupplier = StubClientSession::new;

        MainMenuFrame frame = GuiActionRunner.execute(() -> new MainMenuFrame(sessionSupplier));
        window = new FrameFixture(frame);
        window.show(); // muestra el frame
        window.robot().waitForIdle();
    }

    @AfterEach
    void tearDown() {
        if (window != null)
            window.cleanUp();

        // cerrar cualquier otro frame abierto por los tests
        disposeRemainingFrames();
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
        window.robot().waitForIdle();

        boolean menuVisible = GuiActionRunner.execute(() -> window.target().isVisible());
        assertFalse(menuVisible, "El menú principal debe cerrarse");

        GameHostFrame hostFrame = waitForVisibleFrame(GameHostFrame.class);
        assertNotNull(hostFrame, "GameHostFrame debería abrirse al pulsar Crear partida");
        GuiActionRunner.execute(hostFrame::dispose);
    }

    @Test
    @DisplayName("Click en 'Unirse a partida' cierra el menú y abre GameJoinFrame")
    void shouldOpenJoinFrameOnClickJoin() {
        window.button("unirseBtn").click();
        window.robot().waitForIdle();

        boolean menuVisible = GuiActionRunner.execute(() -> window.target().isVisible());
        assertFalse(menuVisible, "El menú principal debe cerrarse");

        GameJoinFrame joinFrame = waitForVisibleFrame(GameJoinFrame.class);
        assertNotNull(joinFrame, "GameJoinFrame debería abrirse al pulsar Unirse a partida");
        GuiActionRunner.execute(joinFrame::dispose);
    }

    private <T extends Frame> T waitForVisibleFrame(Class<T> frameType) {
        AtomicReference<T> frameRef = new AtomicReference<>();
        Pause.pause(new Condition("Esperando " + frameType.getSimpleName()) {
            @Override
            public boolean test() {
                return GuiActionRunner.execute(() -> {
                    for (Frame frame : Frame.getFrames()) {
                        if (frameType.isInstance(frame) && frame.isShowing()) {
                            frameRef.set(frameType.cast(frame));
                            return true;
                        }
                    }
                    return false;
                });
            }
        }, Timeout.timeout(5_000));
        return frameRef.get();
    }

    private void disposeRemainingFrames() {
        GuiActionRunner.execute(() -> {
            for (Frame frame : Frame.getFrames()) {
                if (frame.isDisplayable()) {
                    frame.dispose();
                }
            }
        });
    }
}
