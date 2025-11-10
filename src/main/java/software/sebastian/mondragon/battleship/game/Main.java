package software.sebastian.mondragon.battleship.game;

import software.sebastian.mondragon.battleship.game.client.GameClientSession;
import software.sebastian.mondragon.battleship.game.server.TcpServer;
import software.sebastian.mondragon.battleship.ui.MainMenuFrame;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
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

    private static void execute(String[] args) throws IOException {
        if (args.length == 0) {
            startClient("localhost", DEFAULT_PORT);
            return;
        }

        String mode = args[0];
        if ("client".equalsIgnoreCase(mode)) {
            String host = args.length > 1 ? args[1] : "localhost";
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
            LOGGER.log(Level.SEVERE, "Entrada inválida: {0}. Usa 'client [host] [port]' o 'server [port]'.", ex.getMessage());
        }
    }

    private static void startServer(int port) throws IOException {
        startServer(port, new CountDownLatch(1));
    }

    static void startServer(int port, CountDownLatch latch) throws IOException {
        TcpServer server = new TcpServer(port);
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
        final String sanitizedHost = (host == null || host.isBlank()) ? "localhost" : host;
        LOGGER.log(Level.INFO, "Iniciando cliente Battleship contra {0}:{1}",
                new Object[]{sanitizedHost, port});
        SwingUtilities.invokeLater(() ->
                new MainMenuFrame(() -> new GameClientSession(sanitizedHost, port)));
    }
}
