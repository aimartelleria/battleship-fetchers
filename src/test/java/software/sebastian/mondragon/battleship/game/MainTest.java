package software.sebastian.mondragon.battleship.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import software.sebastian.mondragon.battleship.game.server.TcpServer;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private static final Method PARSE_PORT_METHOD = resolveParsePort();

    @AfterEach
    void resetMainHooks() {
        Main.resetTestHooks();
    }

    @Test
    void testMainServerConPuertoInvalidoRegistraErrorGlobal() throws Exception {
        List<String> logs = captureMainLogs(Level.SEVERE, () -> Main.main(new String[]{"server", "no-numero"}));
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Error inesperado")), "Esperaba log de error global");
    }

    @Test
    void testParsePortValidaValoresCorrectosYErrores() throws Exception {
        assertEquals(8080, invokeParsePort("8080"));
        IllegalArgumentException rango = assertThrows(IllegalArgumentException.class, () -> invokeParsePort("70000"));
        assertTrue(rango.getMessage().contains("Puerto fuera de rango"));

        IllegalArgumentException invalido = assertThrows(IllegalArgumentException.class, () -> invokeParsePort("abc"));
        assertTrue(invalido.getMessage().contains("Puerto inv"), invalido.getMessage());
    }

    @Test
    void testStartServerDesdeMainSeDetieneCuandoSeInterrumpe() throws Exception {
        int port = allocatePort();
        List<String> logs = runServerScenario(Level.ALL, "main-server-thread", port, String.valueOf(port));

        assertServerLifecycleLogs(logs);
    }

    @Test
    void testModoServerConPuertoPersonalizado() throws Exception {
        int port = allocatePort();
        List<String> logs = runServerScenario(Level.ALL, "main-server-thread-server", port, "server", String.valueOf(port));

        assertServerLifecycleLogs(logs);
    }

    @Test
    void testModoClientConParametrosEjecutaLauncher() throws Exception {
        List<String> lanzamientos = captureClientLaunch("client", "servidor.example", "12345");
        assertEquals(List.of("servidor.example:12345"), lanzamientos);
    }

    @Test
    void testModoClientConHostVacioUsaValoresPorDefecto() throws Exception {
        List<String> lanzamientos = captureClientLaunch("client", "");
        assertEquals(List.of("localhost:9090"), lanzamientos);
    }

    @Test
    void testModoServerSinPuertoUsaPuertoPorDefecto() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        AtomicInteger capturedPort = new AtomicInteger(-1);
        Main.overrideServerFactory(port -> new StubTcpServer(port, started, stopped, capturedPort));

        Thread serverThread = new Thread(() -> Main.main(new String[]{"server"}), "main-server-default-port");
        serverThread.start();

        assertTrue(started.await(5, TimeUnit.SECONDS), "El servidor no se inici�� a tiempo");
        assertEquals(9090, capturedPort.get(), "Debe usar el puerto por defecto 9090");

        serverThread.interrupt();
        serverThread.join(5000);

        assertTrue(stopped.await(2, TimeUnit.SECONDS), "El servidor stub debi�� detenerse");
    }

    @Test
    void testStartServerConLatchPersonalizado() throws Exception {
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        CountDownLatch latch = new CountDownLatch(0);
        assertDoesNotThrow(() -> Main.startServer(port, latch));
    }

    @Test
    void testModoPuertoDirectoInvalidoRegistraErrorEspecifico() throws Exception {
        List<String> logs = captureMainLogs(Level.SEVERE, () -> Main.main(new String[]{"no-es-numero"}));
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Entrada invalida")),
                "Esperaba log indicando error de entrada");
    }

    private static Method resolveParsePort() {
        try {
            Method method = Main.class.getDeclaredMethod("parsePort", String.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("No se encontro el metodo parsePort", ex);
        }
    }

    private static int invokeParsePort(String raw) throws Exception {
        try {
            return (int) PARSE_PORT_METHOD.invoke(null, raw);
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target instanceof Exception exception) {
                throw exception;
            }
            if (target instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(target);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static List<String> captureMainLogs(Level level, ThrowingRunnable action) throws Exception {
        Logger logger = Logger.getLogger(Main.class.getName());
        CapturingHandler handler = new CapturingHandler();
        boolean originalUseParentHandlers = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(level);
        try {
            action.run();
            return handler.snapshot();
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalUseParentHandlers);
            logger.setLevel(originalLevel);
            handler.close();
        }
    }

    private List<String> captureClientLaunch(String... args) throws Exception {
        List<String> launches = new ArrayList<>();
        Main.overrideClientHooks(runnable -> runnable.run(), (host, port) ->
                launches.add(host + ":" + port));
        Main.main(args);
        return launches;
    }

    private void assertServerLifecycleLogs(List<String> logs) {
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Servidor TCP escuchando")));
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Servidor interrumpido")));
    }

    private List<String> runServerScenario(Level level, String threadName, int port, String... args) throws Exception {
        return captureMainLogs(level, () -> startAndStopServer(threadName, port, args));
    }

    private void startAndStopServer(String threadName, int port, String... args) {
        Thread serverThread = new Thread(() -> Main.main(args), threadName);
        serverThread.start();
        try {
            waitUntilPortOpen(port);
            waitUntilThreadWaiting(serverThread);
        } catch (Exception e) {
            serverThread.interrupt();
            throw new RuntimeException(e);
        }
        interruptAndAwaitShutdown(serverThread);
    }

    private void interruptAndAwaitShutdown(Thread serverThread) {
        serverThread.interrupt();
        try {
            serverThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (serverThread.isAlive()) {
            throw new AssertionError("El hilo del servidor no finalizo tras la interrupcion");
        }
    }

    private int allocatePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    private static void waitUntilPortOpen(int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try (Socket ignored = new Socket("127.0.0.1", port)) {
                return;
            } catch (IOException ex) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
            }
        }
        throw new IOException("El servidor no inicio a tiempo en el puerto " + port);
    }

    private static void waitUntilThreadWaiting(Thread thread){
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        throw new AssertionError("El hilo del servidor no entro en estado WAITING");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class CapturingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();
        private final SimpleFormatter formatter = new SimpleFormatter();

        private CapturingHandler() {
            setFormatter(formatter);
        }

        @Override
        public void publish(LogRecord recordBi) {
            if (recordBi != null) {
                messages.add(getFormatter().format(recordBi));
            }
        }

        @Override
        public void flush() {
            // nothing to flush
        }

        @Override
        public void close() {
            messages.clear();
        }

        List<String> snapshot() {
            return new ArrayList<>(messages);
        }
    }

    private static final class StubTcpServer extends TcpServer {
        private final CountDownLatch startedLatch;
        private final CountDownLatch stoppedLatch;
        private final AtomicInteger capturedPort;

        StubTcpServer(int port, CountDownLatch startedLatch, CountDownLatch stoppedLatch, AtomicInteger capturedPort) {
            super(port);
            this.startedLatch = startedLatch;
            this.stoppedLatch = stoppedLatch;
            this.capturedPort = capturedPort;
            this.capturedPort.set(port);
        }

        @Override
        public void start() {
            startedLatch.countDown();
        }

        @Override
        public void stop() {
            stoppedLatch.countDown();
        }
    }
}
