package software.sebastian.mondragon.battleship.game.client;

import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;
import software.sebastian.mondragon.battleship.game.support.AbstractTcpServerIntegrationTest;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests wiring {@link GameClientSession} against a live TCP server.
 */
class GameClientSessionIntegrationTest extends AbstractTcpServerIntegrationTest {
    @Test
    void ensureJugadorAndCrearPartidoOperateOverTcp() throws Exception {
        try (GameClientSession session = new GameClientSession("127.0.0.1", port, Duration.ofSeconds(2))) {
            assertFalse(session.isConnected(), "La sesión no debería conectarse antes de usarse");

            int jugadorId = session.ensureJugador();
            assertTrue(session.isConnected());
            assertEquals(jugadorId, session.getJugadorId());

            int cached = session.ensureJugador();
            assertEquals(jugadorId, cached, "El mismo jugador debería reutilizarse");

            int gameId = session.crearPartido();
            assertEquals(gameId, session.getPartidoId());

            List<String> welcome = session.getWelcomeMessages();
            assertFalse(welcome.isEmpty(), "La sesión debería conservar mensajes de bienvenida");
            assertTrue(welcome.stream().anyMatch(line -> line.contains("WELCOME Battleship TCP")),
                    "Debería incluir el banner de bienvenida real");
        }
    }

    @Test
    void twoClientsReceiveNotificationsAndCanPlayTurns() throws Exception {
        List<String> hostNotifications = new CopyOnWriteArrayList<>();
        List<String> guestNotifications = new CopyOnWriteArrayList<>();

        try (GameClientSession host = new GameClientSession("127.0.0.1", port, Duration.ofSeconds(3));
             GameClientSession guest = new GameClientSession("127.0.0.1", port, Duration.ofSeconds(3))) {
            host.agregarSuscriptorNotificaciones(hostNotifications::add);
            guest.agregarSuscriptorNotificaciones(guestNotifications::add);

            int hostGameId = host.crearPartido();
            int guestGameId = guest.unirsePartido(hostGameId);
            assertEquals(hostGameId, guestGameId, "Ambos clientes deben coincidir en el partido");

            awaitCondition(
                    () -> hostNotifications.stream().anyMatch(msg -> msg.contains("Partida " + hostGameId + " iniciada")),
                    Duration.ofSeconds(2),
                    () -> "Notificaciones anfitrión: " + hostNotifications);
            awaitCondition(
                    () -> guestNotifications.stream().anyMatch(msg -> msg.contains("Te has unido a la partida " + hostGameId)),
                    Duration.ofSeconds(2),
                    () -> "Notificaciones invitado: " + guestNotifications);

            TcpClient.ShipPlacementResult hostShip = host.colocarBarco(List.of(new int[]{0, 0}, new int[]{0, 1}));
            TcpClient.ShipPlacementResult guestShip = guest.colocarBarco(List.of(new int[]{5, 5}, new int[]{5, 6}));
            assertEquals(2, hostShip.size());
            assertEquals(2, guestShip.size());

            ResultadoDisparo result = host.disparar(5, 5);
            assertTrue(EnumSet.of(ResultadoDisparo.AGUA, ResultadoDisparo.TOCADO, ResultadoDisparo.HUNDIDO).contains(result),
                    "El disparo debe producir un resultado válido pero fue: " + result);
        }
    }

    private static void awaitCondition(BooleanSupplier condition, Duration timeout, Supplier<String> failureDetails) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
        }
        fail("Tiempo de espera agotado. " + failureDetails.get());
    }
}
