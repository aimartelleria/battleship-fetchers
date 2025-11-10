package software.sebastian.mondragon.battleship.game.client;

import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class TcpClientTest {

    @Test
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new TcpClient("", 9090));
        assertThrows(IllegalArgumentException.class, () -> new TcpClient("localhost", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TcpClient("localhost", 9090, Duration.ZERO, Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class,
                () -> new TcpClient("localhost", 9090, Duration.ofMillis(100), Duration.ofMillis(-1)));
    }

   

  

    
  



    @Test
    void invalidNumberTriggersException() throws Exception {
        ScriptedServer server = new ScriptedServer(List.of());
        server.addStep(ScriptStep.of("CREATE_PLAYER", "PLAYER not-a-number"));

        try (server; TcpClient client = new TcpClient("127.0.0.1", server.port())) {
            client.connect();
            TcpClientException ex = assertThrows(TcpClientException.class, client::createPlayer);
            assertTrue(ex.getMessage().contains("Invalid number"), ex.getMessage());
        }
    }

    @Test
    void placeShipInputValidation() {
        TcpClient client = new TcpClient("localhost", 9090);
        assertThrows(IllegalArgumentException.class, () -> client.placeShip(null));
        assertThrows(IllegalArgumentException.class, () -> client.placeShip(List.of()));
        assertThrows(IllegalArgumentException.class, () -> client.placeShip(Collections.singletonList(new int[]{1})));
    }

  


    @Test
    void connectionLossQueuesError() throws Exception {
        ScriptedServer server = new ScriptedServer(List.of());
        server.addStep(ScriptStep.closeWithReset("CREATE_PLAYER"));

        try (server; TcpClient client = new TcpClient("127.0.0.1", server.port(), Duration.ofMillis(200), Duration.ofMillis(50))) {
            client.connect();
            TcpClientException ex = assertThrows(TcpClientException.class, client::createPlayer);
            assertTrue(ex.getMessage().contains("Connection lost"), ex.getMessage());
        }
    }

   

    @Test
    void closeIsIdempotent() {
        TcpClient client = new TcpClient("localhost", 9090);
        client.close();
        client.close();
    }

    @Test
    void handshakeTimeoutZeroSkipsCollection() throws Exception {
        ScriptedServer server = new ScriptedServer(List.of());

        try (server; TcpClient client = new TcpClient("127.0.0.1", server.port(),
                Duration.ofSeconds(1), Duration.ZERO)) {
            client.connect();
            assertTrue(client.getWelcomeMessages().isEmpty());
        }
    }



    private static void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Condition not satisfied in time");
    }

    private static final class ScriptedServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final Thread thread;
        private final BlockingQueue<ScriptStep> steps = new LinkedBlockingQueue<>();
        private final List<String> handshakeLines;
        private final CountDownLatch connectedLatch = new CountDownLatch(1);
        private final AtomicBoolean running = new AtomicBoolean(true);

        private volatile PrintWriter writer;
        private volatile Socket clientSocket;
        private volatile Throwable failure;

        ScriptedServer(List<String> handshakeLines) throws IOException {
            this.handshakeLines = handshakeLines;
            this.serverSocket = new ServerSocket(0);
            this.thread = new Thread(this::run, "scripted-tcp-server");
            this.thread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void addStep(ScriptStep step) {
            steps.add(step);
        }

        void awaitClientReady(Duration timeout) throws InterruptedException {
            connectedLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void sendNotification(String message) {
            PrintWriter out = writer;
            if (out != null) {
                out.println("NOTIFY " + message);
            }
        }

        @Override
        public void close() throws Exception {
            running.set(false);
            closeQuietly(clientSocket);
            closeQuietly(serverSocket);
            thread.join(5_000);
            if (failure != null) {
                if (failure instanceof Exception exception) {
                    throw exception;
                }
                if (failure instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(failure);
            }
        }

        private void run() {
            try {
                Socket socket = serverSocket.accept();
                this.clientSocket = socket;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                    this.writer = out;
                    connectedLatch.countDown();

                    for (String line : handshakeLines) {
                        out.println(line);
                    }

                    while (running.get()) {
                        String command = reader.readLine();
                        if (command == null) {
                            break;
                        }
                        ScriptStep step = steps.poll(5, TimeUnit.SECONDS);
                        if (step == null) {
                            failure = new AssertionError("Unexpected command: " + command);
                            break;
                        }
                        step.verify(command);
                        for (String notify : step.notificationsBefore()) {
                            out.println("NOTIFY " + notify);
                        }
                        for (String response : step.responses()) {
                            if (response.isEmpty()) {
                                out.println();
                            } else {
                                out.println(response);
                            }
                        }
                        if (step.forceReset() && !socket.isClosed()) {
                            socket.setSoLinger(true, 0);
                        }
                        if (step.closeConnection()) {
                            socket.close();
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                if (!(t instanceof IOException)) {
                    failure = t;
                }
            } finally {
                running.set(false);
                closeQuietly(writer);
                closeQuietly(clientSocket);
            }
        }
    }

    private record ScriptStep(String expectedCommand, List<String> responses,
                              List<String> notificationsBefore, boolean closeConnection,
                              boolean forceReset) {

        ScriptStep {
            responses = List.copyOf(responses);
            notificationsBefore = List.copyOf(notificationsBefore);
        }

        static ScriptStep of(String expectedCommand, String... responses) {
            return new ScriptStep(expectedCommand, List.of(responses), List.of(), false, false);
        }

        static ScriptStep withNotifications(String expectedCommand, List<String> notifications, String... responses) {
            return new ScriptStep(expectedCommand, List.of(responses), notifications, false, false);
        }

        static ScriptStep closeWithReset(String expectedCommand) {
            return new ScriptStep(expectedCommand, List.of(), List.of(), true, true);
        }

        static ScriptStep noResponse(String expectedCommand) {
            return new ScriptStep(expectedCommand, List.of(), List.of(), false, false);
        }

        void verify(String command) {
            if (!Objects.equals(expectedCommand, command)) {
                throw new AssertionError("Expected command '" + expectedCommand + "' but received '" + command + "'");
            }
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // ignored
            }
        }
    }

    private static void closeQuietly(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // ignored
            }
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // ignored
            }
        }
    }
}
