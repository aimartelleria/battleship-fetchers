package software.sebastian.mondragon.battleship.game.client;

import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * High-level session helper that wraps {@link TcpClient} and keeps track of the current player/game.
 */
public class GameClientSession implements ClientSession {
    private final TcpClient client;
    private final List<Consumer<String>> notificationSubscribers = new CopyOnWriteArrayList<>();
    private final String host;
    private final int port;

    private Integer jugadorId;
    private Integer partidoId;

    public GameClientSession(String host, int port) {
        this(new TcpClient(host, port), host, port);
    }

    public GameClientSession(String host, int port, Duration timeout) {
        this(new TcpClient(host, port, timeout, timeout.dividedBy(10)), host, port);
    }

    public GameClientSession(TcpClient client) {
        this(client, null, -1);
    }

    private GameClientSession(TcpClient client, String host, int port) {
        this.client = Objects.requireNonNull(client, "client");
        this.host = host;
        this.port = port;
        this.client.setNotificationListener(this::dispatchNotification);
    }

    @Override
    public void connect() throws IOException {
        if (!client.isConnected()) {
            client.connect();
        }
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public int ensureJugador() throws IOException, TcpClientException {
        connect();
        if (jugadorId == null) {
            jugadorId = client.createPlayer();
        }
        return jugadorId;
    }

    @Override
    public void usarJugador(int jugadorId) throws IOException, TcpClientException {
        connect();
        this.jugadorId = client.usePlayer(jugadorId);
    }

    @Override
    public int crearPartido() throws IOException, TcpClientException {
        ensureJugador();
        partidoId = client.createGame();
        return partidoId;
    }

    @Override
    public int unirsePartido(int gameId) throws IOException, TcpClientException {
        ensureJugador();
        partidoId = client.joinGame(gameId);
        return partidoId;
    }

    @Override
    public TcpClient.ShipPlacementResult colocarBarco(List<int[]> coords) throws IOException, TcpClientException {
        ensureJugador();
        return client.placeShip(coords);
    }

    @Override
    public ResultadoDisparo disparar(int fila, int columna) throws IOException, TcpClientException {
        if (partidoId == null) {
            throw new IllegalStateException("Debe unirse o crear un partido antes de disparar");
        }
        ensureJugador();
        return client.shoot(partidoId, fila, columna);
    }

    @Override
    public void agregarSuscriptorNotificaciones(Consumer<String> listener) {
        if (listener != null) {
            notificationSubscribers.add(listener);
        }
    }

    @Override
    public void quitarSuscriptorNotificaciones(Consumer<String> listener) {
        notificationSubscribers.remove(listener);
    }

    /**
     * Returns server welcome banner lines captured during handshake.
     */
    @Override
    public List<String> getWelcomeMessages() {
        return client.getWelcomeMessages();
    }

    @Override
    public Integer getJugadorId() {
        return jugadorId;
    }

    @Override
    public Integer getPartidoId() {
        return partidoId;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    private void dispatchNotification(String message) {
        for (Consumer<String> subscriber : notificationSubscribers) {
            subscriber.accept(message);
        }
    }

    @Override
    public void close() {
        try {
            if (client.isConnected()) {
                client.quit();
            }
        } catch (Exception ignored) {
            // No need to propagate errors during shutdown
        } finally {
            client.close();
        }
    }
}
