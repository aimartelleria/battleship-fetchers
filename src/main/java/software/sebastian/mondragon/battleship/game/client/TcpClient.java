package software.sebastian.mondragon.battleship.game.client;

import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TcpClient implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(TcpClient.class.getName());

    // ---------------- Constants ----------------
    private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_HANDSHAKE_TIMEOUT = Duration.ofMillis(500);

    private static final String PREFIX_NOTIFY = "NOTIFY ";
    private static final String PREFIX_ERROR = "ERROR ";

    // Commands
    private static final String CMD_CREATE_PLAYER = "CREATE_PLAYER";
    private static final String CMD_USE_PLAYER = "USE_PLAYER";
    private static final String CMD_CREATE_GAME = "CREATE_GAME";
    private static final String CMD_JOIN_GAME = "JOIN_GAME";
    private static final String CMD_LIST_GAMES = "LIST_GAMES";
    private static final String CMD_PLACE_SHIP = "PLACE_SHIP";
    private static final String CMD_SHOOT = "SHOOT";
    private static final String CMD_QUIT = "QUIT";

    // Response types
    private static final String RESP_PLAYER = "PLAYER";
    private static final String RESP_GAME = "GAME";
    private static final String RESP_JOINED = "JOINED";
    private static final String RESP_GAMES = "GAMES";
    private static final String RESP_SHIP = "SHIP";
    private static final String RESP_RESULT = "RESULT";
    private static final String RESP_BYE = "BYE";

    // ---------------- Fields ----------------
    private final String host;
    private final int port;
    private final Duration responseTimeout;
    private final Duration handshakeTimeout;

    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private final List<String> welcomeMessages = new CopyOnWriteArrayList<>();
    private final Object sendLock = new Object();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicReference<Consumer<String>> notificationListener = new AtomicReference<>(msg -> { });

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread listenerThread;
    private boolean listening;

    // ---------------- Constructors ----------------
    public TcpClient(String host, int port) {
        this(host, port, DEFAULT_RESPONSE_TIMEOUT, DEFAULT_HANDSHAKE_TIMEOUT);
    }

    public TcpClient(String host, int port, Duration responseTimeout, Duration handshakeTimeout) {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Host is required");
        if (port <= 0 || port > 65535) throw new IllegalArgumentException("Port must be between 1-65535");

        this.host = host;
        this.port = port;
        this.responseTimeout = Objects.requireNonNullElse(responseTimeout, DEFAULT_RESPONSE_TIMEOUT);
        this.handshakeTimeout = Objects.requireNonNullElse(handshakeTimeout, DEFAULT_HANDSHAKE_TIMEOUT);

        if (this.responseTimeout.isZero() || this.responseTimeout.isNegative())
            throw new IllegalArgumentException("Response timeout must be positive");
        if (this.handshakeTimeout.isNegative())
            throw new IllegalArgumentException("Handshake timeout cannot be negative");
    }

    // ---------------- Public Methods ----------------
    public synchronized void connect() throws IOException {
        if (connected.get()) throw new IllegalStateException("Client already connected");

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), (int) Math.min(Integer.MAX_VALUE, responseTimeout.toMillis()));

        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        listening = true;
        listenerThread = new Thread(this::notificationLoop, "tcp-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        connected.set(true);
        collectWelcomeMessages();
    }

    public void setNotificationListener(Consumer<String> listener) {
        notificationListener.set(listener != null ? listener : msg -> {});
    }

    public List<String> getWelcomeMessages() {
        return Collections.unmodifiableList(new ArrayList<>(welcomeMessages));
    }

    public boolean isConnected() {
        return connected.get();
    }

    public int createPlayer() throws IOException, TcpClientException {
        return executeCommandInt(CMD_CREATE_PLAYER, RESP_PLAYER, "player id");
    }

    public int usePlayer(int playerId) throws IOException, TcpClientException {
        return executeCommandInt(CMD_USE_PLAYER + " " + playerId, RESP_PLAYER, "player id");
    }

    public int createGame() throws IOException, TcpClientException {
        return executeCommandInt(CMD_CREATE_GAME, RESP_GAME, "game id");
    }

    public int joinGame(int gameId) throws IOException, TcpClientException {
        return executeCommandInt(CMD_JOIN_GAME + " " + gameId, RESP_JOINED, "game id");
    }

    public List<String> listGames() throws  TcpClientException {
        TcpResponse resp = sendCommand(CMD_LIST_GAMES);
        ensureType(resp, RESP_GAMES);
        if (resp.payload().isBlank()) return List.of();
        String[] parts = resp.payload().split("\\|");
        List<String> games = new ArrayList<>(parts.length);
        for (String part : parts) games.add(part.strip());
        return games;
    }

    public ShipPlacementResult placeShip(List<int[]> coordinates) throws  TcpClientException {
        if (coordinates == null || coordinates.isEmpty())
            throw new IllegalArgumentException("At least one coordinate is required");

        StringBuilder cmd = new StringBuilder(CMD_PLACE_SHIP);
        for (int[] c : coordinates) {
            if (c == null || c.length != 2) throw new IllegalArgumentException("Coordinates must have row,col");
            cmd.append(' ').append(c[0]).append(',').append(c[1]);
        }

        TcpResponse resp = sendCommand(cmd.toString());
        ensureType(resp, RESP_SHIP);
        String[] parts = resp.payload().split("\\s+");
        if (parts.length < 3 || !"SIZE".equals(parts[1]))
            throw new TcpClientException("Unexpected SHIP response: " + resp.raw());
        return new ShipPlacementResult(parseInt(parts[0], "ship id"), parseInt(parts[2], "ship size"));
    }

    public ResultadoDisparo shoot(int gameId, int row, int col) throws  TcpClientException {
        TcpResponse resp = sendCommand(CMD_SHOOT + " " + gameId + " " + row + " " + col);
        ensureType(resp, RESP_RESULT);
        try {
            return ResultadoDisparo.valueOf(resp.payload());
        } catch (IllegalArgumentException ex) {
            throw new TcpClientException("Unknown shot result: " + resp.payload(), ex);
        }
    }

    public void quit() throws  TcpClientException {
        TcpResponse resp = sendCommand(CMD_QUIT);
        ensureType(resp, RESP_BYE);
    }

    @Override
    public void close() {
        listening = false;
        connected.set(false);
        if (listenerThread != null) listenerThread.interrupt();
        closeResources();
    }

    // ---------------- Private Helpers ----------------
    private TcpResponse sendCommand(String cmd) throws TcpClientException {
        if (!connected.get() || socket == null || socket.isClosed())
            throw new IllegalStateException("Client not connected");
        writeCommand(cmd);
        return parseLine(awaitResponse());
    }

    private void writeCommand(String cmd) {
        synchronized (sendLock) {
            writer.println(cmd);
            writer.flush();
        }
    }

    private String awaitResponse() throws TcpClientException {
        long remaining = responseTimeout.toNanos();
        while (remaining > 0) {
            long start = System.nanoTime();
            try {
                String line = responseQueue.poll(remaining, TimeUnit.NANOSECONDS);
                remaining -= (System.nanoTime() - start);
                if (line == null) continue;
                if (line.startsWith(PREFIX_ERROR)) throw new TcpClientException(line.substring(PREFIX_ERROR.length()).trim());
                if (!line.isBlank()) return line;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TcpClientException("Interrupted while waiting for server response", e);
            }
        }
        throw new TcpClientException("Timed out waiting for server response");
    }

    private void collectWelcomeMessages() {
        if (handshakeTimeout.isZero()) return;
        long deadline = System.nanoTime() + handshakeTimeout.toNanos();
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) return;
            try {
                String line = responseQueue.poll(remaining, TimeUnit.NANOSECONDS);
                if (line == null) return;
                welcomeMessages.add(line);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void notificationLoop() {
        try {
            String line;
            while (listening && (line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                if (line.startsWith(PREFIX_NOTIFY)) {
                    notificationListener.get().accept(line.substring(PREFIX_NOTIFY.length()));
                } else {
                    enqueueResponseSafely(line);
                }
            }
        } catch (IOException e) {
            enqueueResponseSafely(PREFIX_ERROR + " Connection lost: " + e.getMessage());
        } finally {
            listening = false;
            connected.set(false);
            closeResources();
        }
    }

    private void enqueueResponseSafely(String line) {
        try {
            responseQueue.put(line);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Interrupted while enqueuing message: " + line);
        }
    }

    private void closeResources() {
        closeQuietly(reader);
        if (writer != null) writer.close();
        closeQuietly(socket);
    }

    private int executeCommandInt(String cmd, String respType, String label) throws TcpClientException {
        TcpResponse resp = sendCommand(cmd);
        ensureType(resp, respType);
        return parseInt(resp.payload(), label);
    }

    private static void ensureType(TcpResponse resp, String expected) throws TcpClientException {
        if (!expected.equalsIgnoreCase(resp.type()))
            throw new TcpClientException("Expected " + expected + ", got: " + resp.raw());
    }

    private static int parseInt(String raw, String label) throws TcpClientException {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new TcpClientException("Invalid number for " + label + ": " + raw, e);
        }
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // Ignored because this is a cleanup operation; nothing we can do at this point
            }
        }
    }



    private static TcpResponse parseLine(String line) {
        line = line.trim();
        int idx = line.indexOf(' ');
        String type = idx == -1 ? line : line.substring(0, idx);
        String payload = idx == -1 ? "" : line.substring(idx + 1);
        return new TcpResponse(line, type, payload);
    }

    // ---------------- Nested Classes ----------------
    private record TcpResponse(String raw, String type, String payload) {}
    public record ShipPlacementResult(int shipId, int size) {}
}
