package software.sebastian.mondragon.battleship.game.client.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Minimal TCP server used by unit tests to simulate protocol responses expected by {@link software.sebastian.mondragon.battleship.game.client.TcpClient}.
 */
public final class FakeBattleshipServer implements AutoCloseable {
    private final List<String> handshakeLines;
    private final Function<String, String> responder;
    private final AtomicReference<PrintWriter> writerRef = new AtomicReference<>();
    private final AtomicReference<Socket> clientRef = new AtomicReference<>();
    private final CountDownLatch clientReady = new CountDownLatch(1);

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread thread;
    private int port;

    public FakeBattleshipServer(List<String> handshakeLines, Function<String, String> responder) {
        this.handshakeLines = handshakeLines == null ? List.of() : List.copyOf(handshakeLines);
        this.responder = Objects.requireNonNull(responder, "responder");
    }

    public void start() throws IOException {
        if (thread != null) {
            throw new IllegalStateException("Server already started");
        }
        serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        running = true;
        thread = new Thread(this::acceptLoop, "fake-battleship-server");
        thread.setDaemon(true);
        thread.start();
    }

    public int getPort() {
        if (serverSocket == null) {
            throw new IllegalStateException("Server not started yet");
        }
        return port;
    }

    public void awaitClientConnected(Duration timeout) throws InterruptedException {
        long millis = timeout.toMillis();
        if (!clientReady.await(millis, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("Client did not connect within " + timeout);
        }
    }

    public void sendNotification(String message) {
        PrintWriter writer = writerRef.get();
        if (writer != null) {
            writer.println("NOTIFY " + message);
            writer.flush();
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // best effort shutdown
        }
        Socket socket = clientRef.get();
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
        if (thread != null) {
            try {
                thread.join(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void acceptLoop() {
        try (Socket socket = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            clientRef.set(socket);
            writerRef.set(writer);
            for (String line : handshakeLines) {
                writer.println(line);
            }
            clientReady.countDown();

            String line;
            while (running && (line = reader.readLine()) != null) {
                String response = responder.apply(line);
                if (response != null) {
                    writer.println(response);
                }
                if ("QUIT".equalsIgnoreCase(line)) {
                    running = false;
                }
            }
        } catch (IOException ignored) {
            // socket closed during shutdown
        } finally {
            clientReady.countDown();
        }
    }
}
