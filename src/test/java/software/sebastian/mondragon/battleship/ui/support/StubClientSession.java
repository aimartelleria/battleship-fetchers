package software.sebastian.mondragon.battleship.ui.support;

import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.game.client.TcpClient;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Sencilla implementación de {@link ClientSession} para pruebas de UI sin red.
 */
public class StubClientSession implements ClientSession {
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final List<String> welcomeMessages;
    private int jugadorId;
    private int partidoId;
    private int shipCounter = 0;
    private boolean connected;

    public StubClientSession() {
        this(1, List.of("WELCOME Battleship TCP", "Type HELP for available commands."));
    }

    public StubClientSession(int jugadorId, List<String> welcomeMessages) {
        this.jugadorId = jugadorId;
        this.welcomeMessages = new ArrayList<>(welcomeMessages);
    }

    @Override
    public void connect() throws IOException {
        connected = true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public int ensureJugador() {
        return jugadorId;
    }

    @Override
    public void usarJugador(int jugadorId) {
        this.jugadorId = jugadorId;
    }

    @Override
    public int crearPartido() {
        partidoId = 100 + jugadorId;
        return partidoId;
    }

    @Override
    public int unirsePartido(int gameId) {
        partidoId = gameId;
        return partidoId;
    }

    @Override
    public TcpClient.ShipPlacementResult colocarBarco(List<int[]> coords) {
        return new TcpClient.ShipPlacementResult(++shipCounter, coords.size());
    }

    @Override
    public ResultadoDisparo disparar(int fila, int columna) {
        return ResultadoDisparo.AGUA;
    }

    @Override
    public void agregarSuscriptorNotificaciones(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void quitarSuscriptorNotificaciones(Consumer<String> listener) {
        listeners.remove(listener);
    }

    public void emitNotification(String message) {
        listeners.forEach(listener -> listener.accept(message));
    }

    @Override
    public List<String> getWelcomeMessages() {
        return List.copyOf(welcomeMessages);
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
        return "localhost";
    }

    @Override
    public int getPort() {
        return 9090;
    }

    @Override
    public void close() {
        connected = false;
    }
}

