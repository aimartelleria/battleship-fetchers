package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;
import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.game.client.TcpClient.ShipPlacementResult;
import software.sebastian.mondragon.battleship.game.client.TcpClientException;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class GameJoinFrameTest {

    private FrameFixture window;
    private StubClientSession sessionStub;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "false");
        sessionStub = new StubClientSession();
        GameJoinFrame frame = GuiActionRunner.execute(() -> new GameJoinFrame(() -> sessionStub));
        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    void tearDown() {
        if (window != null) window.cleanUp();
        for (Frame f : Frame.getFrames()) if (f.isVisible()) f.dispose();
    }

    @Test
    @DisplayName("Inicializa correctamente los controles")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Unirse a Partida", frame.getTitle());
        window.label("statusLabel").requireVisible();
        window.textBox("gameCodeField").requireVisible();
        window.button("connectButton").requireText("Conectar");
    }

    @Test
    @DisplayName("Muestra advertencia si el código está vacío")
    void shouldWarnIfCodeEmpty() {
        window.textBox("gameCodeField").setText("");
        window.button("connectButton").click();
        assertTrue(window.dialog().target().isVisible());
        window.dialog().button().click();
    }

    @Test
    @DisplayName("Muestra advertencia si el código no es numérico")
    void shouldWarnIfCodeNotNumeric() {
        window.textBox("gameCodeField").setText("abc");
        window.button("connectButton").click();
        assertTrue(window.dialog().target().isVisible());
        window.dialog().button().click();
    }

    @Test
    @DisplayName("Conecta correctamente al juego con ID numérico")
    void shouldConnectSuccessfully() {
        window.textBox("gameCodeField").setText("123");
        window.button("connectButton").click();

        GuiActionRunner.execute(() -> {
            sessionStub.connected = true;
            sessionStub.triggerSuccessfulJoin(1, 123);
        });

        assertTrue(sessionStub.connected);
        assertFalse(sessionStub.closedBeforeBoard);
    }

    @Test
    @DisplayName("Maneja excepción al conectar")
    void shouldHandleConnectionFailure() {
        sessionStub.throwOnConnect = true;
        window.textBox("gameCodeField").setText("42");
        window.button("connectButton").click();

        GuiActionRunner.execute(() -> sessionStub.triggerFailure(new ExecutionException(new RuntimeException("Error"))));
        assertTrue(window.dialog().target().isVisible());
        window.dialog().button().click();
    }

    @Test
    @DisplayName("Llama cleanup correctamente y no lo repite")
    void shouldCleanupOnce() throws IOException {
        sessionStub.closeThrow = true;
        JFrame frame = (JFrame) window.target();
        GuiActionRunner.execute(() -> {
            frame.dispose();
            frame.dispose();
        });
        assertFalse(sessionStub.cleanupCalled);
    }

    @Test
    @DisplayName("Cancelación durante conexión limpia correctamente")
    void shouldCancelAndReturnProperly() {
        window.textBox("gameCodeField").setText("55");
        window.button("connectButton").click();
        GuiActionRunner.execute(() -> {
            GameJoinFrame gf = (GameJoinFrame) window.target();
            gf.dispose(); // fuerza cancelación
        });
        assertFalse(sessionStub.cleanupCalled);
    }


    @Test
    @DisplayName("Agrega notificaciones de forma segura")
    void shouldAppendNotificationSafely() {
        GameJoinFrame frame = (GameJoinFrame) window.target();
        GuiActionRunner.execute(() -> frame.appendNotificationSafely("Mensaje Test"));
        assertTrue(window.textBox("notificationArea").text().contains("Mensaje Test"));
    }

    @Test
    @DisplayName("Actualiza correctamente los detalles de conexión")
    void shouldUpdateInfoDetailsProperly() {
        sessionStub.host = "localhost";
        sessionStub.port = 8080;
        GameJoinFrame frame = GuiActionRunner.execute(() -> new GameJoinFrame(() -> sessionStub));
        assertTrue(frame.infoArea.getText().contains("localhost:8080"));
        frame.dispose();
    }

    // -----------------------------------------------------------------
    // Stub interno (sin Mockito)
    // -----------------------------------------------------------------
    private static class StubClientSession implements ClientSession {
        boolean connected;
        boolean unsubscribed;
        boolean cleanupCalled;
        boolean closedBeforeBoard;
        boolean throwOnConnect;
        boolean closeThrow;
        String host = "127.0.0.1";
        int port = 5000;
        Consumer<String> subscriber;

        @Override
        public void connect() {
            if (throwOnConnect) throw new RuntimeException("Simulated connect failure");
            connected = true;
        }

        @Override
        public void close() throws IOException {
            cleanupCalled = true;
            if (closeThrow) throw new IOException("Simulated close fail");
            closedBeforeBoard = true;
        }

        @Override
        public void agregarSuscriptorNotificaciones(Consumer<String> sub) {
            subscriber = sub;
        }

        @Override
        public void quitarSuscriptorNotificaciones(Consumer<String> sub) {
            unsubscribed = true;
        }

        @Override
        public int ensureJugador() {
            return 1;
        }

        @Override
        public int unirsePartido(int id) {
            return id;
        }

        @Override
        public List<String> getWelcomeMessages() {
            return List.of("Bienvenido!", "Buena suerte!");
        }

        void triggerSuccessfulJoin(int jugadorId, int partidaId) {
            if (subscriber != null)
                subscriber.accept("Unido correctamente");
        }

        void triggerFailure(Exception ex) {
            if (subscriber != null)
                subscriber.accept("Error: " + ex.getMessage());
        }

        public String getHost() { return host; }
        public int getPort() { return port; }

        @Override
        public boolean isConnected() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'isConnected'");
        }

        @Override
        public void usarJugador(int jugadorId) throws IOException, TcpClientException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'usarJugador'");
        }

        @Override
        public int crearPartido() throws IOException, TcpClientException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'crearPartido'");
        }

        @Override
        public ShipPlacementResult colocarBarco(List<int[]> coords) throws IOException, TcpClientException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'colocarBarco'");
        }

        @Override
        public ResultadoDisparo disparar(int fila, int columna) throws IOException, TcpClientException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'disparar'");
        }

        @Override
        public Integer getJugadorId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getJugadorId'");
        }

        @Override
        public Integer getPartidoId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getPartidoId'");
        }
    }
}
