package software.sebastian.mondragon.battleship.game.client;

import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class GameClientSessionTest {

    @Test
    void ensureJugadorConnectsOnceAndCachesId() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.nextPlayerId = 15;

        try (GameClientSession session = new GameClientSession(client)) {
            int first = session.ensureJugador();
            int second = session.ensureJugador();

            assertEquals(15, first);
            assertEquals(first, second);
            assertEquals(1, client.connectCalls);
            assertEquals(1, client.createPlayerCalls);
        }
    }

    @Test
    void usarJugadorDelegatesAndOverridesCachedPlayer() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.usePlayerResult = 42;

        try (GameClientSession session = new GameClientSession(client)) {
            session.usarJugador(99);

            assertEquals(42, session.getJugadorId());
            assertEquals(1, client.usePlayerCalls);
            assertEquals(99, client.lastUsePlayerArg);
            assertEquals(0, client.createPlayerCalls);
        }
    }

    @Test
    void crearYUnirsePartidoAssignIdsAfterEnsuringJugador() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.nextPlayerId = 3;
        client.createGameResult = 77;
        client.joinGameResult = 88;

        try (GameClientSession session = new GameClientSession(client)) {
            int created = session.crearPartido();
            assertEquals(77, created);
            assertEquals(created, session.getPartidoId());
            assertEquals(1, client.createGameCalls);
            assertEquals(1, client.createPlayerCalls, "createPartido debe garantizar jugador");

            int joined = session.unirsePartido(10);
            assertEquals(88, joined);
            assertEquals(joined, session.getPartidoId());
            assertEquals(1, client.joinGameCalls);
            assertEquals(10, client.lastJoinGameArg);
            assertEquals(1, client.createPlayerCalls, "la unión reutiliza el jugador");
        }
    }

    @Test
    void colocarBarcoEnsuresJugadorAntesDeDelegar() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.shipPlacementResult = new TcpClient.ShipPlacementResult(9, 3);

        try (GameClientSession session = new GameClientSession(client)) {
            TcpClient.ShipPlacementResult result = session.colocarBarco(List.of(new int[]{0, 0}, new int[]{0, 1}));

            assertEquals(9, result.shipId());
            assertEquals(3, result.size());
            assertEquals(1, client.createPlayerCalls);
            assertEquals(1, client.placeShipCalls);
            assertEquals(List.of("0,0", "0,1"), client.lastPlacedShipCoords);
        }
    }

    @Test
    void dispararRequiresPartidoSeleccionado() {
        StubTcpClient client = new StubTcpClient();

        try (GameClientSession session = new GameClientSession(client)) {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> session.disparar(0, 0));
            assertTrue(ex.getMessage().contains("unirse o crear un partido"));
        }
    }

    @Test
    void dispararDelegatesUsingStoredPartidoId() throws Exception {
        StubTcpClient client = new StubTcpClient();
        client.joinGameResult = 55;
        client.shootResult = ResultadoDisparo.HUNDIDO;

        try (GameClientSession session = new GameClientSession(client)) {
            session.unirsePartido(55);
            ResultadoDisparo result = session.disparar(4, 5);

            assertEquals(ResultadoDisparo.HUNDIDO, result);
            assertEquals(1, client.shootCalls);
            assertEquals(55, client.lastShootGameId);
            assertEquals(4, client.lastShootRow);
            assertEquals(5, client.lastShootCol);
        }
    }

    @Test
    void notificationsOnlyReachRegisteredSubscribers() {
        StubTcpClient client = new StubTcpClient();
        List<String> receivedA = new CopyOnWriteArrayList<>();
        List<String> receivedB = new CopyOnWriteArrayList<>();

        try (GameClientSession session = new GameClientSession(client)) {
            Consumer<String> listenerB = receivedB::add;
            session.agregarSuscriptorNotificaciones(receivedA::add);
            session.agregarSuscriptorNotificaciones(listenerB);

            client.emitNotification("turno");
            assertEquals(List.of("turno"), receivedA);
            assertEquals(List.of("turno"), receivedB);

            session.quitarSuscriptorNotificaciones(listenerB);
            client.emitNotification("final");
            assertEquals(List.of("turno", "final"), receivedA);
            assertEquals(List.of("turno"), receivedB);
        }
    }

    @Test
    void closeInvokesQuitAndAlwaysClosesClient()  {
        StubTcpClient client = new StubTcpClient();
        client.connected = true;
        client.quitException = new TcpClientException("boom");

        GameClientSession session = new GameClientSession(client);
        assertDoesNotThrow(session::close);

        assertEquals(1, client.quitCalls);
        assertEquals(1, client.closeCalls);
        assertFalse(client.connected, "el cliente debe marcarse desconectado");
    }

    private static final class StubTcpClient extends TcpClient {
        private boolean connected;
        private int connectCalls;
        private int closeCalls;
        private int quitCalls;
        private int createPlayerCalls;
        private int usePlayerCalls;
        private int createGameCalls;
        private int joinGameCalls;
        private int placeShipCalls;
        private int shootCalls;
        private TcpClientException quitException;
        private Consumer<String> notificationListener = message -> { };

        private int nextPlayerId = 1;
        private int usePlayerResult = 1;
        private int createGameResult = 10;
        private int joinGameResult = 11;
        private TcpClient.ShipPlacementResult shipPlacementResult =
                new TcpClient.ShipPlacementResult(1, 2);
        private ResultadoDisparo shootResult = ResultadoDisparo.AGUA;
        private final List<String> welcomeMessages = List.of("WELCOME Battleship TCP");

        private Integer lastUsePlayerArg;
        private Integer lastJoinGameArg;
        private List<String> lastPlacedShipCoords = List.of();
        private int lastShootGameId;
        private int lastShootRow;
        private int lastShootCol;

        private StubTcpClient() {
            super("127.0.0.1", 9090, Duration.ofSeconds(1), Duration.ZERO);
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
            return nextPlayerId;
        }

        @Override
        public int usePlayer(int playerId) {
            usePlayerCalls++;
            lastUsePlayerArg = playerId;
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
            lastJoinGameArg = gameId;
            return joinGameResult;
        }

        @Override
        public TcpClient.ShipPlacementResult placeShip(List<int[]> coordinates) {
            placeShipCalls++;
            List<String> formatted = new ArrayList<>();
            for (int[] coord : coordinates) {
                formatted.add(coord[0] + "," + coord[1]);
            }
            lastPlacedShipCoords = formatted;
            return shipPlacementResult;
        }

        @Override
        public ResultadoDisparo shoot(int gameId, int row, int col) {
            shootCalls++;
            lastShootGameId = gameId;
            lastShootRow = row;
            lastShootCol = col;
            return shootResult;
        }

        @Override
        public void quit() throws TcpClientException {
            quitCalls++;
            if (quitException != null) {
                throw quitException;
            }
            connected = false;
        }

        @Override
        public void close() {
            closeCalls++;
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

        private void emitNotification(String message) {
            notificationListener.accept(message);
        }
    }
}
