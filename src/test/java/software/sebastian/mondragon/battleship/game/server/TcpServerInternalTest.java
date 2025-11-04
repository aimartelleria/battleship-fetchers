package software.sebastian.mondragon.battleship.game.server;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import software.sebastian.mondragon.battleship.game.service.GameService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests que cubren escenarios internos y rutas de error del {@link TcpServer}
 * que no resultan f&aacute;ciles de provocar mediante pruebas de integraci&oacute;n.
 */
class TcpServerInternalTest {

    @Test
    void getGameServiceDevuelveInstancia() {
        TcpServer server = new TcpServer(0);
        assertNotNull(server.getGameService());
    }

    @Test
    void startIgnoraSegundaInvocacionCuandoYaEstaCorriendo() throws Exception {
        TcpServer server = new TcpServer(0);
        try {
            server.start();
            assertDoesNotThrow(server::start);
        } finally {
            server.stop();
        }
    }

    @Test
    void stopManejaIOExceptionAlCerrarServerSocket() throws Exception {
        TcpServer server = new TcpServer(0);
        ExplodingServerSocket exploding = new ExplodingServerSocket();
        setField(server, "serverSocket", exploding);
        setField(server, "running", true);

        assertDoesNotThrow(server::stop);
        assertTrue(exploding.closeInvocado);
    }

    @Test
    void stopReinterrumpeHiloActualCuandoJoinEsInterrumpido() throws Exception {
        TcpServer server = new TcpServer(0);
        Thread acceptThread = new Thread(() -> {
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "accept-stub");
        acceptThread.setDaemon(true);
        acceptThread.start();

        setField(server, "acceptThread", acceptThread);
        setField(server, "running", true);

        Thread.currentThread().interrupt();
        try {
            server.stop();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // limpia el flag para otros tests
            acceptThread.interrupt();
        }
    }

    @Test
    void acceptLoopRegistraErrorCuandoAcceptFalla() throws Exception {
        TcpServer server = new TcpServer(0);
        FailingServerSocket failing = new FailingServerSocket(server);
        setField(server, "serverSocket", failing);
        setField(server, "running", true);

        Method acceptLoop = TcpServer.class.getDeclaredMethod("acceptLoop");
        acceptLoop.setAccessible(true);
        acceptLoop.invoke(server);

        assertTrue(failing.aceptados >= 2);
    }

    @Test
    void clientHandlerCapturaIOExceptionDuranteProcesamiento() throws Exception {
        TcpServer server = new TcpServer(0);
        ThrowingInputSocket socket = new ThrowingInputSocket();
        Object handler = newClientHandler(server, socket);

        Runnable runnable = (Runnable) handler;
        assertDoesNotThrow(runnable::run);
        assertTrue(socket.cerrado);
    }

    @Test
    void clientHandlerCapturaExcepcionInesperadaYEnviaError() throws Exception {
        TcpServer server = new TcpServer(0);
        FailOnLineSocket socket = new FailOnLineSocket("HELP\n");
        Object handler = newClientHandler(server, socket);

        Runnable runnable = (Runnable) handler;
        assertDoesNotThrow(runnable::run);
        assertTrue(socket.getSalida().toString(StandardCharsets.UTF_8)
                .contains("Unexpected error"));
    }

    @Test
    void notifierEntregaMensajeCuandoHandlerRegistrado() throws Exception {
        TcpServer server = new TcpServer(0);
        RecordingSocket socket = new RecordingSocket("");
        Object handler = newClientHandler(server, socket);

        Map<Integer, Object> clients = clientsMap(server);
        int playerId = 42;
        setHandlerPlayerId(handler, playerId);
        clients.put(playerId, handler);

        GameService.Notifier notifier = extractNotifier(server.getGameService());
        notifier.notifyJugador(playerId, "ping");

        assertTrue(socket.getOutput(StandardCharsets.UTF_8).contains("NOTIFY ping"));

        invokeCloseQuietly(handler);
    }

    @Test
    void processClientCommandsIgnoraLineasVacias() throws Exception {
        TcpServer server = new TcpServer(0);
        RecordingSocket socket = new RecordingSocket("   \nHELP\nQUIT\n");
        Object handler = newClientHandler(server, socket);

        Runnable runnable = (Runnable) handler;
        assertDoesNotThrow(runnable::run);

        String salida = socket.getOutput(StandardCharsets.UTF_8);
        assertTrue(salida.contains("COMMANDS:"));
        assertTrue(salida.contains("BYE"));
    }

    @Test
    void colocarBarcoSinCoordenadasLanzaError() throws Exception {
        TcpServer server = new TcpServer(0);
        RecordingSocket socket = new RecordingSocket("");
        Object handler = newClientHandler(server, socket);

        Method colocarBarco = handler.getClass().getDeclaredMethod("colocarBarco", String[].class);
        colocarBarco.setAccessible(true);
        Object client = clientHandler(handler);
        String[] tokens = new String[]{"PLACE_SHIP"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> invoke(client, colocarBarco, tokens));
        assertTrue(ex.getMessage().contains("Debe especificar"));
    }

    @Test
    void dispararConTokensInsuficientesLanzaError() throws Exception {
        TcpServer server = new TcpServer(0);
        RecordingSocket socket = new RecordingSocket("");
        Object handler = newClientHandler(server, socket);

        Method disparar = handler.getClass().getDeclaredMethod("disparar", String[].class);
        disparar.setAccessible(true);
        Object client = clientHandler(handler);
        String[] tokens = new String[]{"SHOOT", "1"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> invoke(client, disparar, tokens));
        assertTrue(ex.getMessage().contains("Uso: SHOOT"));
    }

    @Test
    void closeQuietlyIgnoraIOExceptionAlCerrarSocket() throws Exception {
        TcpServer server = new TcpServer(0);
        FailingCloseSocket socket = new FailingCloseSocket();
        Object handler = newClientHandler(server, socket);

        Method closeQuietly = handler.getClass().getDeclaredMethod("closeQuietly");
        closeQuietly.setAccessible(true);
        assertDoesNotThrow(() -> closeQuietly.invoke(handler));
        assertTrue(socket.closeInvocado);
    }

    @Test
    void asociarJugadorReemplazaYSuprimeAnterior() throws Exception {
        TcpServer server = new TcpServer(0);
        Class<?> handlerClass = Class.forName("software.sebastian.mondragon.battleship.game.server.TcpServer$ClientHandler");
        Method asociar = handlerClass.getDeclaredMethod("asociarJugador", int.class);
        asociar.setAccessible(true);

        RecordingSocket socketOriginal = new RecordingSocket("");
        Object handlerOriginal = newClientHandler(server, socketOriginal);
        asociar.invoke(handlerOriginal, 7);

        RecordingSocket socketReplacement = new RecordingSocket("");
        Object handlerReplacement = newClientHandler(server, socketReplacement);
        asociar.invoke(handlerReplacement, 7);

        String originalSalida = socketOriginal.getOutput(StandardCharsets.UTF_8);
        assertTrue(originalSalida.contains("Sesion") || originalSalida.contains("Sesi"));

        asociar.invoke(handlerReplacement, 8);

        Map<Integer, Object> clients = clientsMap(server);
        assertFalse(clients.containsKey(7));
        assertSame(handlerReplacement, clients.get(8));

        invokeCloseQuietly(handlerOriginal);
        invokeCloseQuietly(handlerReplacement);
    }

    private static Object clientHandler(Object handler) {
        return handler;
    }

    private static Object invoke(Object target, Method method, String[] tokens) throws Exception {
        try {
            return method.invoke(target, (Object) tokens);
        } catch (InvocationTargetException ex) {
            Throwable targetException = ex.getTargetException();
            if (targetException instanceof Exception exception) {
                throw exception;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        }
    }

    private static Object newClientHandler(TcpServer server, Socket socket) throws Exception {
        Class<?> handlerClass = Class.forName("software.sebastian.mondragon.battleship.game.server.TcpServer$ClientHandler");
        Constructor<?> constructor = handlerClass.getDeclaredConstructor(TcpServer.class, Socket.class);
        constructor.setAccessible(true);
        return constructor.newInstance(server, socket);
    }

    private static Map<Integer, Object> clientsMap(TcpServer server) throws Exception {
        Field field = TcpServer.class.getDeclaredField("clientsByPlayer");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, Object> map = (Map<Integer, Object>) field.get(server);
        return map;
    }

    private static void setHandlerPlayerId(Object handler, int playerId) throws Exception {
        Field field = handler.getClass().getDeclaredField("playerId");
        field.setAccessible(true);
        field.set(handler, playerId);
    }

    private static GameService.Notifier extractNotifier(GameService service) throws Exception {
        Field field = GameService.class.getDeclaredField("notifier");
        field.setAccessible(true);
        return (GameService.Notifier) field.get(service);
    }

    private static void invokeCloseQuietly(Object handler) throws Exception {
        Method method = handler.getClass().getDeclaredMethod("closeQuietly");
        method.setAccessible(true);
        method.invoke(handler);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = TcpServer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class ExplodingServerSocket extends ServerSocket {
        boolean closeInvocado;

        ExplodingServerSocket() throws IOException {
            super();
        }

        @Override
        public void close() throws IOException {
            closeInvocado = true;
            throw new IOException("fallo forzado");
        }
    }

    private static final class FailingServerSocket extends ServerSocket {
        private final TcpServer owner;
        int aceptados;

        FailingServerSocket(TcpServer owner) throws IOException {
            super();
            this.owner = owner;
        }

        @Override
        public Socket accept() throws IOException {
            aceptados++;
            if (aceptados == 1) {
                throw new IOException("fallo en accept");
            }
            setRunning(owner, false);
            throw new SocketException("socket cerrado");
        }
    }

    private static final class ThrowingInputSocket extends Socket {
        private final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("lectura fallida");
            }
        };
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean cerrado;

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public synchronized void close() {
            cerrado = true;
        }
    }

    private static final class FailOnLineSocket extends Socket {
        private final InputStream input;
        private final LineCountingOutputStream output;

        FailOnLineSocket(String script) {
            this.input = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
            this.output = new LineCountingOutputStream(3);
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        LineCountingOutputStream getSalida() {
            return output;
        }

        @Override
        public synchronized void close() {
            // no-op
        }
    }

    private static final class RecordingSocket extends Socket {
        private final InputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        RecordingSocket(String script) {
            this.input = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        String getOutput(java.nio.charset.Charset charset) {
            return output.toString(charset);
        }

        @Override
        public synchronized void close() {
            // nada que cerrar
        }
    }

    private static final class FailingCloseSocket extends Socket {
        private final ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean closeInvocado;

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public synchronized void close() throws IOException {
            closeInvocado = true;
            throw new IOException("fallo en cierre");
        }
    }

    private static final class LineCountingOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private final int failAtLine;
        private int lineCounter;
        private boolean failed;

        LineCountingOutputStream(int failAtLine) {
            this.failAtLine = failAtLine;
        }

        @Override
        public void write(int b) {
            if (!failed && b == '\n') {
                lineCounter++;
                if (lineCounter >= failAtLine) {
                    failed = true;
                    throw new RuntimeException("falla simulada");
                }
            }
            delegate.write(b);
        }

        String toString(java.nio.charset.Charset charset) {
            return delegate.toString(charset);
        }
    }

    private static void setRunning(TcpServer server, boolean value) throws RuntimeException {
        try {
            Field field = TcpServer.class.getDeclaredField("running");
            field.setAccessible(true);
            field.setBoolean(server, value);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
