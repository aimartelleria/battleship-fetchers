package software.sebastian.mondragon.battleship.game;

import software.sebastian.mondragon.battleship.game.client.GameClientSession;
import software.sebastian.mondragon.battleship.game.server.TcpServer;
import software.sebastian.mondragon.battleship.ui.MainMenuFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int DEFAULT_PORT = 9090;
    private static Consumer<Runnable> uiExecutor = SwingUtilities::invokeLater;
    private static ClientLauncher clientLauncher = Main::launchDefaultClient;
    private static TcpServerFactory serverFactory = TcpServer::new;
    private static final String DEFAULT_HOST = "localhost";

    public static void main(String[] args) {
        try {
            execute(args);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error inesperado en la aplicación", ex);
        }
    }

    private static void execute(String[] args) throws IOException {
        if (args.length == 0) {
            startClient(DEFAULT_HOST, DEFAULT_PORT);
            return;
        }

        String mode = args[0];
        if ("client".equalsIgnoreCase(mode)) {
            String host = args.length > 1 ? args[1] : DEFAULT_HOST;
            int port = args.length > 2 ? parsePort(args[2]) : DEFAULT_PORT;
            startClient(host, port);
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
            LOGGER.log(Level.SEVERE, "Entrada invalida: {0}. Usa 'client [host] [port]' o 'server [port]'.", ex.getMessage());
        }
    }

    private static void startServer(int port) throws IOException {
        startServer(port, new CountDownLatch(1));
    }

    static void startServer(int port, CountDownLatch latch) throws IOException {
        TcpServer server = serverFactory.create(port);
        server.start();
        LOGGER.log(Level.INFO, "Servidor TCP escuchando en el puerto {0}. Presiona Ctrl+C para detenerlo.", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Deteniendo servidor TCP...");
            server.stop();
        }));

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

    private static void startClient(String host, int port) {
        final String sanitizedHost = (host == null || host.isBlank()) ? DEFAULT_HOST : host;
        LOGGER.log(Level.INFO, "Iniciando cliente Battleship contra {0}:{1}",
                new Object[]{sanitizedHost, port});
        uiExecutor.accept(() -> clientLauncher.launch(sanitizedHost, port));
    }

    private static void launchDefaultClient(String host, int port) {
        new MainMenuFrame(() -> new GameClientSession(host, port));
    }

    static void overrideClientHooks(Consumer<Runnable> executorOverride,
                                    ClientLauncher launcherOverride) {
        uiExecutor = Objects.requireNonNullElse(executorOverride, SwingUtilities::invokeLater);
        clientLauncher = Objects.requireNonNullElse(launcherOverride, Main::launchDefaultClient);
    }


    static void overrideServerFactory(TcpServerFactory factory) {
        serverFactory = factory != null ? factory : TcpServer::new;
    }

    static void resetTestHooks() {
        overrideClientHooks( null, null);
        overrideServerFactory(null);
    }

    @FunctionalInterface
    interface ClientLauncher {
        void launch(String host, int port);
    }

    @FunctionalInterface
    interface TcpServerFactory {
        TcpServer create(int port) throws IOException;
    }
}

