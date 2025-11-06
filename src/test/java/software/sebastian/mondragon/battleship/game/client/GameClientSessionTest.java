package software.sebastian.mondragon.battleship.game.client;

import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class GameClientSessionTest {

    @Test
    void ensureJugadorCreatesPlayerOnlyOnce() throws Exception {
        StubTcpClient client = new StubTcpClient();
        GameClientSession session = new GameClientSession(client);

        int first = session.ensureJugador();
        int second = session.ensureJugador();

        assertEquals(client.createPlayerResult, first);
        assertEquals(first, second);
        assertEquals(1, client.createPlayerCalls);
        assertEquals(1, client.connectCalls);
    }

    @Test
    void usarJugadorDelegatesToClient() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.usePlayerResult = 77;
        GameClientSession session = new GameClientSession(client);

        session.usarJugador(55);

        assertEquals(1, client.usePlayerCalls);
        assertEquals(55, client.usePlayerArgument);
        assertEquals(77, session.getJugadorId());
    }

    @Test
    void crearPartidoEnsuresJugadorAndStoresGameId() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.createGameResult = 222;
        GameClientSession session = new GameClientSession(client);

        int gameId = session.crearPartido();

        assertEquals(222, gameId);
        assertEquals(1, client.createPlayerCalls);
        assertEquals(1, client.createGameCalls);
        assertEquals(222, session.getPartidoId());
    }

    @Test
    void unirsePartidoStoresGameId() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.joinGameResult = 333;
        GameClientSession session = new GameClientSession(client);

        int joined = session.unirsePartido(999);

        assertEquals(333, joined);
        assertEquals(999, client.joinGameArgument);
        assertEquals(333, session.getPartidoId());
    }

    @Test
    void colocarBarcoDelegatesToClient() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.shipPlacementResult = new TcpClient.ShipPlacementResult(91, 3);
        GameClientSession session = new GameClientSession(client);

        TcpClient.ShipPlacementResult result = session.colocarBarco(List.of(new int[]{0, 0}));

        assertEquals(91, result.shipId());
        assertEquals(3, result.size());
        assertEquals(1, client.placeShipCalls);
        assertArrayEquals(new int[]{0, 0}, client.lastPlacedShip.get(0));
    }

    @Test
    void dispararWithoutPartidoThrows() {
        StubTcpClient client = new StubTcpClient();
        GameClientSession session = new GameClientSession(client);

        assertThrows(IllegalStateException.class, () -> session.disparar(1, 1));
    }

    @Test
    void dispararDelegatesWhenPartidoAssigned() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.shootResult = ResultadoDisparo.TOCADO;
        GameClientSession session = new GameClientSession(client);
        session.unirsePartido(10); // establishes partidoId

        ResultadoDisparo resultado = session.disparar(4, 5);

        assertEquals(ResultadoDisparo.TOCADO, resultado);
        assertEquals(1, client.shootCalls);
        assertEquals(client.joinGameResult, client.lastShootPartido);
        assertEquals(4, client.lastShootRow);
        assertEquals(5, client.lastShootCol);
    }

    @Test
    void notificationSubscribersReceiveAndCanBeRemoved() {
        StubTcpClient client = new StubTcpClient();
        GameClientSession session = new GameClientSession(client);

        session.agregarSuscriptorNotificaciones(null); // branch where listener is ignored

        List<String> received = new CopyOnWriteArrayList<>();
        Consumer<String> listener = received::add;
        session.agregarSuscriptorNotificaciones(listener);

        client.emitNotification("hola");
        assertEquals(List.of("hola"), received);

        session.quitarSuscriptorNotificaciones(listener);
        client.emitNotification("adios");
        assertEquals(List.of("hola"), received);
    }

    @Test
    void welcomeMessagesAreDelegated() {
        StubTcpClient client = new StubTcpClient();
        client.welcomeMessages = List.of("uno", "dos");
        GameClientSession session = new GameClientSession(client);

        assertEquals(List.of("uno", "dos"), session.getWelcomeMessages());
    }

    @Test
    void closeInvokesQuitAndCloseEvenOnException() throws Exception {
        StubTcpClient client = new StubTcpClient();
        GameClientSession session = new GameClientSession(client);

        client.connected = true;
        session.close();
        assertTrue(client.quitCalled);
        assertTrue(client.closeCalled);
        assertFalse(client.connected);

        client.quitCalled = false;
        client.closeCalled = false;
        client.connected = true;
        client.throwOnQuit = true;
        session.close();
        assertTrue(client.quitCalled);
        assertTrue(client.closeCalled);
        assertFalse(client.connected);

        session.close(); // branch where client is already disconnected
        assertTrue(client.closeCalled);
    }

    @Test
    void connectDoesNotReconnectWhenAlreadyConnected() throws Exception {
        StubTcpClient client = new StubTcpClient();
        GameClientSession session = new GameClientSession(client);

        session.connect();
        assertTrue(session.isConnected());
        session.connect();

        assertEquals(1, client.connectCalls);
    }

    @Test
    void constructorsExposeHostAndPort() {
        try (GameClientSession session = new GameClientSession("localhost", 12345)) {
            assertEquals("localhost", session.getHost());
            assertEquals(12345, session.getPort());
        }

        try (GameClientSession session = new GameClientSession("127.0.0.1", 54321, Duration.ofSeconds(1))) {
            assertEquals("127.0.0.1", session.getHost());
            assertEquals(54321, session.getPort());
        }
    }

    private static final class StubTcpClient extends TcpClient {
        boolean connected;
        boolean throwOnQuit;
        boolean quitCalled;
        boolean closeCalled;
        int connectCalls;
        int createPlayerCalls;
        int createPlayerResult = 42;
        int usePlayerCalls;
        int usePlayerArgument;
        int usePlayerResult = 52;
        int createGameCalls;
        int createGameResult = 61;
        int joinGameCalls;
        int joinGameArgument;
        int joinGameResult = 71;
        int placeShipCalls;
        List<int[]> lastPlacedShip = new ArrayList<>();
        TcpClient.ShipPlacementResult shipPlacementResult = new TcpClient.ShipPlacementResult(1, 1);
        int shootCalls;
        int lastShootPartido;
        int lastShootRow;
        int lastShootCol;
        ResultadoDisparo shootResult = ResultadoDisparo.AGUA;
        List<String> welcomeMessages = List.of();
        Consumer<String> notificationListener = message -> { };

        StubTcpClient() {
            super("localhost", 12345, Duration.ofSeconds(1), Duration.ofMillis(100));
        }

        @Override
        public synchronized void connect() {
            connectCalls++;
            connected = true;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public int createPlayer() {
            createPlayerCalls++;
            return createPlayerResult;
        }

        @Override
        public int usePlayer(int playerId) {
            usePlayerCalls++;
            usePlayerArgument = playerId;
            return usePlayerResult;
        }

        @Override
        public int createGame() {
            createGameCalls++;
            return createGameResult;
        }

        @Override
        public int joinGame(int gameId) {
            joinGameCalls++;
            joinGameArgument = gameId;
            return joinGameResult;
        }

        @Override
        public ShipPlacementResult placeShip(List<int[]> coordinates) {
            placeShipCalls++;
            lastPlacedShip = coordinates;
            return shipPlacementResult;
        }

        @Override
        public ResultadoDisparo shoot(int gameId, int row, int col) {
            shootCalls++;
            lastShootPartido = gameId;
            lastShootRow = row;
            lastShootCol = col;
            return shootResult;
        }

        @Override
        public void quit() throws IOException {
            quitCalled = true;
            if (throwOnQuit) {
                throw new IOException("boom");
            }
            connected = false;
        }

        @Override
        public void close() {
            closeCalled = true;
            connected = false;
        }

        @Override
        public List<String> getWelcomeMessages() {
            return welcomeMessages;
        }

        @Override
        public void setNotificationListener(Consumer<String> listener) {
            notificationListener = listener != null ? listener : message -> { };
        }

        void emitNotification(String message) {
            notificationListener.accept(message);
        }
    }
}
