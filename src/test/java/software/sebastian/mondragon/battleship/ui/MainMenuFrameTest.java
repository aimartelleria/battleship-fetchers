package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.*;
import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.ui.support.StubClientSession;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuFrameTest {

    private FrameFixture window;
    private Supplier<ClientSession> sessionSupplier;
    private MainMenuFrame frame;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false"); // asegura modo gráfico
        sessionSupplier = StubClientSession::new;

        frame = GuiActionRunner.execute(() -> new MainMenuFrame(sessionSupplier));
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

        frame = null;
        window = null;
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

        waitForMenuToClose();
        assertFalse(GuiActionRunner.execute(() -> frame.isShowing()),
                "El menú principal debe cerrarse");

        Frame hostFrame = waitForVisibleFrame(GameHostFrame.class);
        assertNotNull(hostFrame, "GameHostFrame debería abrirse al pulsar Crear partida");
        GuiActionRunner.execute(hostFrame::dispose);
    }

    @Test
    @DisplayName("Click en 'Unirse a partida' cierra el menú y abre GameJoinFrame")
    void shouldOpenJoinFrameOnClickJoin() {
        window.button("unirseBtn").click();

        waitForMenuToClose();
        assertFalse(GuiActionRunner.execute(() -> frame.isShowing()),
                "El menú principal debe cerrarse");

        Frame joinFrame = waitForVisibleFrame(GameJoinFrame.class);
        assertNotNull(joinFrame, "GameJoinFrame debería abrirse al pulsar Unirse a partida");
        GuiActionRunner.execute(joinFrame::dispose);
    }

    private void waitForMenuToClose() {
        Pause.pause(new Condition("Main menu frame to close") {
            @Override
            public boolean test() {
                return GuiActionRunner.execute(() -> !frame.isDisplayable() || !frame.isShowing());
            }
        }, Timeout.timeout(5, TimeUnit.SECONDS));
    }

    private <T extends Frame> T waitForVisibleFrame(Class<T> frameClass) {
        AtomicReference<T> result = new AtomicReference<>();
        Pause.pause(new Condition(frameClass.getSimpleName() + " visible") {
            @Override
            public boolean test() {
                return GuiActionRunner.execute(() -> {
                    for (Frame f : Frame.getFrames()) {
                        if (frameClass.isInstance(f) && f.isShowing()) {
                            result.set(frameClass.cast(f));
                            return true;
                        }
                    }
                    return false;
                });
            }
        }, Timeout.timeout(5, TimeUnit.SECONDS));
        return result.get();
    }
}

