package software.sebastian.mondragon.battleship.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@link TcpServer}.
 * These tests start the TCP server, connect clients, and verify
 * correct protocol responses and interaction sequences.
 */
class TcpServerTest {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);

    private TcpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        server = new TcpServer(port);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void helpCommandListsAvailableCommands() throws Exception {
        try (ClientConnection client = connectAndGreet()) {
            client.send("HELP");
            assertEquals("COMMANDS:", client.awaitExact("COMMANDS:"));
            assertEquals("  CREATE_PLAYER               -> Crea un nuevo jugador y lo asocia a la sesión.", client.awaitStartsWith("  CREATE_PLAYER"));
            assertEquals("  USE_PLAYER <playerId>       -> Usa un jugador existente.", client.awaitStartsWith("  USE_PLAYER"));
            assertEquals("  CREATE_GAME                 -> Crea una partida con el jugador actual.", client.awaitStartsWith("  CREATE_GAME"));
            assertEquals("  JOIN_GAME <gameId>          -> Une al jugador actual a la partida indicada.", client.awaitStartsWith("  JOIN_GAME"));
            assertEquals("  LIST_GAMES                  -> Lista partidas existentes.", client.awaitStartsWith("  LIST_GAMES"));
            assertEquals("  PLACE_SHIP <fila,col>...    -> Coloca un barco usando coordenadas pares.", client.awaitStartsWith("  PLACE_SHIP"));
            assertEquals("  SHOOT <gameId> <fila> <col> -> Realiza un disparo.", client.awaitStartsWith("  SHOOT"));
            assertEquals("  QUIT                        -> Cierra la conexión.", client.awaitStartsWith("  QUIT"));
        }
    }

    @Test
    void createPlayerAndListGamesFlow() throws Exception {
        try (ClientConnection client = connectAndGreet()) {
            int playerId = createPlayer(client);
            assertEquals(1, playerId);

            client.send("LIST_GAMES");
            assertEquals("GAMES", client.awaitExact("GAMES"));

            String creationNotify = createGame(client);
            assertTrue(creationNotify.contains("Partida creada con id 1"));

            client.send("LIST_GAMES");
            String listLine = client.awaitLine(line -> line.startsWith("GAMES "), "Esperaba lista de partidas");
            assertTrue(listLine.contains("Partido{id=1"), listLine);
        }
    }

    @Test
    void joiningGameSendsNotificationsToBothPlayers() throws Exception {
        try (MatchContext match = startMatch()) {
            assertTrue(match.hostCreatedNotify.contains("Partida creada con id " + match.gameId));
            assertTrue(match.hostStartNotify.contains("Partida " + match.gameId + " iniciada"), match.hostStartNotify);
            assertTrue(match.hostJoinNotify.contains("Jugador " + match.guestId + " se ha unido"), match.hostJoinNotify);

            assertTrue(match.guestStartNotify.contains("Partida " + match.gameId + " iniciada"), match.guestStartNotify);
            assertTrue(match.guestJoinNotify.contains("Te has unido a la partida " + match.gameId), match.guestJoinNotify);
            assertEquals("JOINED " + match.gameId, match.joinLine);
        }
    }

    @Test
    void placeShipCommandPlacesShipForCurrentPlayer() throws Exception {
        try (MatchContext match = startMatch()) {
            match.host.send("PLACE_SHIP 0,0 0,1");
            String shipLine = match.host.awaitLine(line -> line.startsWith("SHIP "), "Esperaba confirmación de barco");
            assertTrue(shipLine.matches("SHIP \\d+ SIZE 2"), shipLine);
        }
    }

    @Test
    void usePlayerFailsForUnknownId() throws Exception {
        try (ClientConnection client = connectAndGreet()) {
            client.send("USE_PLAYER 99");
            String error = client.awaitStartsWith("ERROR ");
            assertTrue(error.contains("Jugador no encontrado"));
        }
    }

    private ClientConnection connectAndGreet() throws Exception {
        ClientConnection client = new ClientConnection(port);
        assertEquals("WELCOME Battleship TCP", client.awaitExact("WELCOME Battleship TCP"));
        assertEquals("Type HELP for available commands.", client.awaitExact("Type HELP for available commands."));
        return client;
    }

    private int createPlayer(ClientConnection client) throws Exception {
        client.send("CREATE_PLAYER");
        String line = client.awaitStartsWith("PLAYER ");
        return Integer.parseInt(line.substring("PLAYER ".length()));
    }

    private String createGame(ClientConnection client) throws Exception {
        client.send("CREATE_GAME");
        String notifyLine = client.awaitStartsWith("NOTIFY ");
        String gameLine = client.awaitStartsWith("GAME ");
        assertNotNull(gameLine);
        assertEquals("GAME 1", gameLine);
        return notifyLine;
    }

    private MatchContext startMatch() throws Exception {
        ClientConnection host = connectAndGreet();
        int hostId = createPlayer(host);

        host.send("CREATE_GAME");
        String createdNotify = host.awaitStartsWith("NOTIFY ");
        String gameLine = host.awaitStartsWith("GAME ");
        int gameId = Integer.parseInt(gameLine.substring("GAME ".length()));

        ClientConnection guest = connectAndGreet();
        int guestId = createPlayer(guest);

        guest.send("JOIN_GAME " + gameId);
        String guestStart = guest.awaitLine(line -> line.contains("Partida " + gameId + " iniciada"), "Esperaba notificación de inicio para invitado");
        String guestJoin = guest.awaitLine(line -> line.contains("Te has unido a la partida " + gameId), "Esperaba confirmación de unión para invitado");
        String joinLine = guest.awaitStartsWith("JOINED ");

        String hostStart = host.awaitLine(line -> line.contains("Partida " + gameId + " iniciada"), "Esperaba notificación de inicio para anfitrión");
        String hostJoin = host.awaitLine(line -> line.contains("Jugador " + guestId + " se ha unido"), "Esperaba notificación de unión para anfitrión");

        return new MatchContext(host, guest, hostId, guestId, gameId, createdNotify, hostStart, hostJoin, guestStart, guestJoin, joinLine);
    }

    private static final class MatchContext implements AutoCloseable {
        final ClientConnection host;
        final ClientConnection guest;
        final int hostId;
        final int guestId;
        final int gameId;
        final String hostCreatedNotify;
        final String hostStartNotify;
        final String hostJoinNotify;
        final String guestStartNotify;
        final String guestJoinNotify;
        final String joinLine;

        private MatchContext(ClientConnection host,
                             ClientConnection guest,
                             int hostId,
                             int guestId,
                             int gameId,
                             String hostCreatedNotify,
                             String hostStartNotify,
                             String hostJoinNotify,
                             String guestStartNotify,
                             String guestJoinNotify,
                             String joinLine) {
            this.host = Objects.requireNonNull(host);
            this.guest = Objects.requireNonNull(guest);
            this.hostId = hostId;
            this.guestId = guestId;
            this.gameId = gameId;
            this.hostCreatedNotify = hostCreatedNotify;
            this.hostStartNotify = hostStartNotify;
            this.hostJoinNotify = hostJoinNotify;
            this.guestStartNotify = guestStartNotify;
            this.guestJoinNotify = guestJoinNotify;
            this.joinLine = joinLine;
        }

        @Override
        public void close() throws Exception {
            host.close();
            guest.close();
        }
    }

    private static final class ClientConnection implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Deque<String> inbox = new ArrayDeque<>();

        ClientConnection(int port) throws IOException {
            this.socket = new Socket("127.0.0.1", port);
            this.socket.setSoTimeout(200);
            this.socket.setTcpNoDelay(true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void send(String command) throws IOException {
            writer.write(command);
            writer.write("\n");
            writer.flush();
        }

        String awaitExact(String expected) throws Exception {
            return awaitLine(expected::equals, "Linea inesperada, esperaba: " + expected);
        }

        String awaitStartsWith(String prefix) throws Exception {
            return awaitLine(line -> line.startsWith(prefix), "Linea no empieza con: " + prefix);
        }

        String awaitLine(Predicate<String> predicate, String failureMessage) throws Exception {
            return awaitLine(predicate, DEFAULT_TIMEOUT, failureMessage);
        }

        String awaitLine(Predicate<String> predicate, Duration timeout, String failureMessage) throws Exception {
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                for (Iterator<String> it = inbox.iterator(); it.hasNext(); ) {
                    String buffered = it.next();
                    if (predicate.test(buffered)) {
                        it.remove();
                        return buffered;
                    }
                }
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        fail(failureMessage + " (conexion cerrada)");
                    }
                    if (predicate.test(line)) {
                        return line;
                    }
                    inbox.addLast(line);
                } catch (SocketTimeoutException ignored) {
                    // Intentionally ignored: retry until timeout expires
                }
            }
            fail(failureMessage + " (timeout). Pendientes: " + inbox);
            return null; // unreachable
        }

        @Override
        public void close() throws Exception {
            try {
                writer.close();
            } catch (IOException ignored) {
                // Intentionally ignored: closing writer during test cleanup
            }
            try {
                reader.close();
            } catch (IOException ignored) {
                // Intentionally ignored: closing reader during test cleanup
            }
            try {
                socket.close();
            } catch (IOException ignored) {
                // Intentionally ignored: closing socket during test cleanup
            }
        }
    }
}
