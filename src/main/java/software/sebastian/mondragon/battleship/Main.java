package software.sebastian.mondragon.battleship;

import software.sebastian.mondragon.battleship.model.Jugador;
import software.sebastian.mondragon.battleship.model.Mapa;
import software.sebastian.mondragon.battleship.model.Partido;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;
import software.sebastian.mondragon.battleship.server.TcpServer;
import software.sebastian.mondragon.battleship.service.GameService;
import software.sebastian.mondragon.battleship.service.GameService.Notifier;
import software.sebastian.mondragon.battleship.service.ResultadoDisparo;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int DEFAULT_PORT = 9090;

    public static void main(String[] args) {
        try {
            execute(args);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error inesperado en la aplicación", ex);
        }
    }

    private static void execute(String[] args) throws IOException, NumberFormatException, IllegalArgumentException {
        if (args.length == 0) {
            runDemo();
            return;
        }

        String mode = args[0];
        if ("demo".equalsIgnoreCase(mode)) {
            runDemo();
            return;
        }

        if ("server".equalsIgnoreCase(mode)) {
            int port = args.length > 1 ? parsePort(args[1]) : DEFAULT_PORT;
            startServer(port);
            return;
        }

        // modo directo: interpretar primer argumento como puerto para conveniencia
        try {
            int port = parsePort(mode);
            startServer(port);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.SEVERE, "Entrada inválida de puerto: {0}", ex.getMessage());
        }
    }

    private static void startServer(int port) throws IOException {
        TcpServer server = new TcpServer(port);
        server.start();
        LOGGER.log(Level.INFO, "Servidor TCP escuchando en el puerto {0}. Presiona Ctrl+C para detenerlo.", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Deteniendo servidor TCP...");
            server.stop();
        }));

        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Servidor interrumpido: {0}", ex.getMessage());
        } finally {
            server.stop();
        }
    }

    private static int parsePort(String raw) {
        try {
            int port = Integer.parseInt(raw);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Puerto fuera de rango (1-65535): " + raw);
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Puerto inválido: " + raw, ex);
        }
    }

    private static void runDemo() {
        InMemoryRepo repo = new InMemoryRepo();

        Notifier notifier = (jugadorId, mensaje) ->
                LOGGER.log(Level.INFO, "[Notificación -> Jugador {0}] {1}", new Object[]{jugadorId, mensaje});

        GameService gs = new GameService(repo, notifier);

        Jugador j1 = gs.crearJugador();
        Jugador j2 = gs.crearJugador();
        LOGGER.log(Level.INFO, "Jugadores creados: {0}, {1}", new Object[]{j1, j2});

        Partido p = gs.crearPartido(j1.getId());
        LOGGER.log(Level.INFO, "Partida creada: {0}", p);

        gs.unirsePartido(p.getId(), j2.getId());

        Mapa mapa1 = repo.getMapa(j1.getMapaId());
        Mapa mapa2 = repo.getMapa(j2.getMapaId());
        validarMapas(mapa1, mapa2);

        gs.colocarBarco(j1.getId(), Arrays.asList(new int[]{0, 0}, new int[]{0, 1}));
        gs.colocarBarco(j2.getId(), Arrays.asList(new int[]{5, 5}, new int[]{5, 6}));

        LOGGER.info("Barcos colocados. Empezamos a disparar...");

        int turno = repo.getPartido(p.getId()).getTurnoJugadorId();
        int other = determinarOtroJugador(turno, j1, j2);

        ejecutarDisparo(gs, turno, p.getId(), 5, 5);
        ejecutarDisparo(gs, other, p.getId(), 0, 0);

        LOGGER.log(Level.INFO, "Demo finalizado. Estado final del partido: {0}", repo.getPartido(p.getId()));
    }

    static int determinarOtroJugador(int turno, Jugador j1, Jugador j2) {
        return (turno == j1.getId()) ? j2.getId() : j1.getId();
    }

    static void ejecutarDisparo(GameService gs, int jugadorId, int partidoId, int fila, int columna) {
        try {
            ResultadoDisparo resultado = gs.disparar(jugadorId, partidoId, fila, columna);
            LOGGER.log(Level.INFO, "Resultado: {0}", resultado);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error en disparo: {0}", ex.getMessage());
        }
    }


    static void validarMapas(Mapa mapa1, Mapa mapa2) {
        if (mapa1 == null || mapa2 == null) {
            throw new IllegalStateException("Mapas no inicializados");
        }
    }
}
