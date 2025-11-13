package software.sebastian.mondragon.battleship.game.client;

import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Lightweight TCP client to interact with {@link software.sebastian.mondragon.battleship.game.server.TcpServer}.
 * <p>
 * The client maintains a single request queue with one outstanding command at a time.
 * Commands are issued through the high level methods (e.g. {@link #createPlayer()})
 * which abstract the textual protocol exposed by {@code TcpServer}.
 * </p>
 *
 * <p>
 * Notifications (lines starting with {@code NOTIFY}) are delivered asynchronously to the provided listener.
 * The client buffers welcome messages sent by the server immediately after connecting so the UI can display them.
 * </p>
 */
public class TcpClient implements Closeable {
    private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_HANDSHAKE_TIMEOUT = Duration.ofMillis(500);

    private final String host;
    private final int port;
    private final Duration responseTimeout;
    private final Duration handshakeTimeout;
    private final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
    private final List<String> welcomeMessages = new CopyOnWriteArrayList<>();
    private final Object sendLock = new Object();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private static final Consumer<String> NO_OP_LISTENER = message -> { };
    private final AtomicReference<Consumer<String>> notificationListener = new AtomicReference<>(NO_OP_LISTENER);
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread listenerThread;
    private volatile boolean listening;

    public TcpClient(String host, int port) {
        this(host, port, DEFAULT_RESPONSE_TIMEOUT, DEFAULT_HANDSHAKE_TIMEOUT);
    }

    public TcpClient(String host, int port, Duration responseTimeout, Duration handshakeTimeout) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host is required");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.host = host;
        this.port = port;
        this.responseTimeout = Objects.requireNonNullElse(responseTimeout, DEFAULT_RESPONSE_TIMEOUT);
        this.handshakeTimeout = Objects.requireNonNullElse(handshakeTimeout, DEFAULT_HANDSHAKE_TIMEOUT);
        if (this.responseTimeout.isZero() || this.responseTimeout.isNegative()) {
            throw new IllegalArgumentException("Response timeout must be positive");
        }
        if (this.handshakeTimeout.isNegative()) {
            throw new IllegalArgumentException("Handshake timeout cannot be negative");
        }
    }

    /**
     * Establishes the TCP connection and starts the background reader.
     */
    public synchronized void connect() throws IOException {
        if (connected.get()) {
            throw new IllegalStateException("Client is already connected");
        }

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), (int) Math.min(Integer.MAX_VALUE, responseTimeout.toMillis()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        listening = true;
        listenerThread = new Thread(this::listenLoop, "battleship-tcp-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        connected.set(true);
        collectWelcomeMessages();
    }

    /**
     * Assigns a callback to receive asynchronous NOTIFY messages.
     */
    public void setNotificationListener(Consumer<String> listener) {
        notificationListener.set(listener != null ? listener : NO_OP_LISTENER);
    }

    /**
     * Returns immutable welcome messages emitted by the server during handshake, if any.
     */
    public List<String> getWelcomeMessages() {
        return Collections.unmodifiableList(new ArrayList<>(welcomeMessages));
    }

    public boolean isConnected() {
        return connected.get();
    }

    public int createPlayer() throws IOException, TcpClientException {
        TcpResponse response = sendCommand("CREATE_PLAYER");
        ensureType(response, "PLAYER");
        return parseInt(response.payload(), "player id");
    }

    public int usePlayer(int playerId) throws IOException, TcpClientException {
        TcpResponse response = sendCommand("USE_PLAYER " + playerId);
        ensureType(response, "PLAYER");
        return parseInt(response.payload(), "player id");
    }

    public int createGame() throws IOException, TcpClientException {
        TcpResponse response = sendCommand("CREATE_GAME");
        ensureType(response, "GAME");
        return parseInt(response.payload(), "game id");
    }

    public int joinGame(int gameId) throws IOException, TcpClientException {
        TcpResponse response = sendCommand("JOIN_GAME " + gameId);
        ensureType(response, "JOINED");
        return parseInt(response.payload(), "game id");
    }

    public List<String> listGames() throws IOException, TcpClientException {
        TcpResponse response = sendCommand("LIST_GAMES");
        ensureType(response, "GAMES");
        if (response.payload().isBlank()) {
            return List.of();
        }
        String[] parts = response.payload().split("\\|");
        List<String> games = new ArrayList<>(parts.length);
        for (String part : parts) {
            games.add(part.strip());
        }
        return games;
    }

    public ShipPlacementResult placeShip(List<int[]> coordinates) throws IOException, TcpClientException {
        if (coordinates == null || coordinates.isEmpty()) {
            throw new IllegalArgumentException("At least one coordinate is required");
        }

        StringBuilder builder = new StringBuilder("PLACE_SHIP");
        for (int[] coord : coordinates) {
            if (coord == null || coord.length != 2) {
                throw new IllegalArgumentException("Coordinates must contain row and column");
            }
            builder.append(' ').append(coord[0]).append(',').append(coord[1]);
        }
        TcpResponse response = sendCommand(builder.toString());
        ensureType(response, "SHIP");

        String[] tokens = response.payload().split("\\s+");
        if (tokens.length < 3 || !"SIZE".equals(tokens[1])) {
            throw new TcpClientException("Unexpected SHIP response: " + response.raw());
        }
        int shipId = parseInt(tokens[0], "ship id");
        int size = parseInt(tokens[2], "ship size");
        return new ShipPlacementResult(shipId, size);
    }

    public ResultadoDisparo shoot(int gameId, int row, int col) throws IOException, TcpClientException {
        TcpResponse response = sendCommand("SHOOT " + gameId + ' ' + row + ' ' + col);
        ensureType(response, "RESULT");
        try {
            return ResultadoDisparo.valueOf(response.payload());
        } catch (IllegalArgumentException ex) {
            throw new TcpClientException("Unknown shot result: " + response.payload(), ex);
        }
    }

    public void quit() throws IOException, TcpClientException {
        TcpResponse response = sendCommand("QUIT");
        ensureType(response, "BYE");
    }

    @Override
    public void close() {
        listening = false;
        connected.set(false);
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        closeQuietly(reader);
        if (writer != null) {
            writer.close();
        }
        closeQuietly(socket);
    }

    private TcpResponse sendCommand(String command) throws IOException, TcpClientException {
        if (!connected.get() || socket == null || socket.isClosed()) {
            throw new IllegalStateException("Client is not connected");
        }
        synchronized (sendLock) {
            writer.println(command);
            writer.flush();
        }
        String line = awaitResponse();
        return parseResponse(line);
    }

    private String awaitResponse() throws TcpClientException {
        long remainingNanos = responseTimeout.toNanos();
        while (remainingNanos > 0) {
            long start = System.nanoTime();
            try {
                String line = inbox.poll(remainingNanos, TimeUnit.NANOSECONDS);
                long elapsed = System.nanoTime() - start;
                remainingNanos -= elapsed;

                if (line == null) {
                    continue;
                }

                if (line.startsWith("ERROR ")) {
                    throw new TcpClientException(line.substring("ERROR ".length()).trim());
                }
                if (!line.isBlank()) {
                    return line;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new TcpClientException("Interrupted while waiting for server response", ex);
            }
        }
        throw new TcpClientException("Timed out waiting for server response");
    }

    private void collectWelcomeMessages() {
        if (handshakeTimeout.isZero()) {
            return;
        }
        long deadline = System.nanoTime() + handshakeTimeout.toNanos();
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return;
            }
            try {
                String line = inbox.poll(remaining, TimeUnit.NANOSECONDS);
                if (line == null) {
                    return;
                }
                welcomeMessages.add(line);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void listenLoop() {
        try {
            String line;
            while (listening && (line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("NOTIFY ")) {
                    notificationListener.get().accept(line.substring("NOTIFY ".length()));
                } else {
                    enqueueLine(line);
                }
            }
        } catch (IOException ex) {
            if (listening) {
                enqueueLine("ERROR Connection lost: " + ex.getMessage());
            }
        } finally {
            listening = false;
            connected.set(false);
            closeQuietly(reader);
            if (writer != null) {
                writer.close();
            }
            closeQuietly(socket);
        }
    }

    private static void ensureType(TcpResponse response, String expected) throws TcpClientException {
        if (!expected.equalsIgnoreCase(response.type())) {
            throw new TcpClientException("Unexpected response. Expected " + expected + " but got: " + response.raw());
        }
    }

    private static int parseInt(String raw, String label) throws TcpClientException {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new TcpClientException("Invalid number for " + label + ": " + raw, ex);
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Ignored on close
            }
        }
    }

    private static void closeQuietly(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                // Ignored on close
            }
        }
    }

    private TcpResponse parseResponse(String line) {
        int idx = line.indexOf(' ');
        String type = idx == -1 ? line : line.substring(0, idx);
        String payload = idx == -1 ? "" : line.substring(idx + 1);
        return new TcpResponse(line, type, payload);
    }

    private void enqueueLine(String line) {
        try {
            inbox.put(line);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while queueing server message", ex);
        }
    }

    private record TcpResponse(String raw, String type, String payload) { }

    public record ShipPlacementResult(int shipId, int size) { }
}
