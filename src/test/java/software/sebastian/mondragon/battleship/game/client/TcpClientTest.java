package software.sebastian.mondragon.battleship.game.client;

import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.game.client.support.FakeBattleshipServer;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class TcpClientTest {

    @Test
    void connectCollectsWelcomeMessagesAndHandlesCommands() throws Exception {
        AtomicInteger playerSequence = new AtomicInteger(7);
        AtomicInteger shipSequence = new AtomicInteger(30);

        withServerAndClient(
                List.of("WELCOME Battleship TCP", "READY"),
                command -> {
                    if ("CREATE_PLAYER".equals(command)) {
                        return "PLAYER " + playerSequence.getAndIncrement();
                    }
                    if ("LIST_GAMES".equals(command)) {
                        return "GAMES Alpha|Beta League";
                    }
                    if (command.startsWith("PLACE_SHIP")) {
                        return "SHIP " + shipSequence.getAndIncrement() + " SIZE 2";
                    }
                    if (command.startsWith("SHOOT")) {
                        return "RESULT TOCADO";
                    }
                    if ("QUIT".equals(command)) {
                        return "BYE Adios";
                    }
                    return "ERROR Unexpected command: " + command;
                },
                (server, client) -> {
                    assertTrue(client.isConnected());
                    assertEquals(List.of("WELCOME Battleship TCP", "READY"), client.getWelcomeMessages());

                    assertEquals(7, client.createPlayer());
                    assertEquals(List.of("Alpha", "Beta League"), client.listGames());

                    TcpClient.ShipPlacementResult ship = client.placeShip(List.of(new int[]{0, 0}, new int[]{0, 1}));
                    assertEquals(2, ship.size());
                    assertTrue(ship.shipId() >= 30);

                    ResultadoDisparo result = client.shoot(123, 4, 5);
                    assertEquals(ResultadoDisparo.TOCADO, result);

                    assertDoesNotThrow(client::quit);
                });
    }

    @Test
    void notificationListenerReceivesAsyncMessages() throws Exception {
        withServerAndClient(
                List.of("WELCOME Battleship TCP"),
                command -> {
                    if ("CREATE_PLAYER".equals(command)) {
                        return "PLAYER 1";
                    }
                    if ("QUIT".equals(command)) {
                        return "BYE bye";
                    }
                    return "ERROR Unexpected: " + command;
                },
                (server, client) -> {
                    CountDownLatch latch = new CountDownLatch(1);
                    client.setNotificationListener(message -> {
                        if ("Turno listo".equals(message)) {
                            latch.countDown();
                        }
                    });

                    server.awaitClientConnected(Duration.ofSeconds(1));
                    server.sendNotification("Turno listo");

                    assertTrue(latch.await(1, TimeUnit.SECONDS), "El listener debe recibir la notificación");
                    client.quit();
                });
    }

    @Test
    void constructorValidatesArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new TcpClient("", 9090, Duration.ofSeconds(1), Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new TcpClient("localhost", 0, Duration.ofSeconds(1), Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new TcpClient("localhost", 9090, Duration.ZERO, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new TcpClient("localhost", 9090, Duration.ofMillis(-1), Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new TcpClient("localhost", 9090, Duration.ofSeconds(1), Duration.ofMillis(-1)));
    }

    private void withServerAndClient(List<String> welcomeBanner,
                                     Function<String, String> handler,
                                     ClientInteraction interaction) throws Exception {
        try (FakeBattleshipServer server = new FakeBattleshipServer(welcomeBanner, handler)) {
            server.start();
            try (TcpClient client = new TcpClient("127.0.0.1", server.getPort(), Duration.ofSeconds(2), Duration.ofMillis(200))) {
                client.connect();
                interaction.accept(server, client);
            }
        }
    }

    @FunctionalInterface
    private interface ClientInteraction {
        void accept(FakeBattleshipServer server, TcpClient client) throws Exception;
    }
}
