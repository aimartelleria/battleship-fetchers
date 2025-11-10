package software.sebastian.mondragon.battleship.game.client;

import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstraction used by la UI Swing para interactuar con un backend de Battleship.
 */
public interface ClientSession extends Closeable {
    void connect() throws IOException;

    boolean isConnected();

    int ensureJugador() throws IOException, TcpClientException;

    void usarJugador(int jugadorId) throws IOException, TcpClientException;

    int crearPartido() throws IOException, TcpClientException;

    int unirsePartido(int gameId) throws IOException, TcpClientException;

    TcpClient.ShipPlacementResult colocarBarco(List<int[]> coords) throws IOException, TcpClientException;

    ResultadoDisparo disparar(int fila, int columna) throws IOException, TcpClientException;

    void agregarSuscriptorNotificaciones(Consumer<String> listener);

    void quitarSuscriptorNotificaciones(Consumer<String> listener);

    List<String> getWelcomeMessages();

    Integer getJugadorId();

    Integer getPartidoId();

    String getHost();

    int getPort();
}

