package software.sebastian.mondragon.battleship.game;

import org.junit.jupiter.api.Test;

import software.sebastian.mondragon.battleship.game.model.Jugador;
import software.sebastian.mondragon.battleship.game.model.Mapa;
import software.sebastian.mondragon.battleship.game.repo.InMemoryRepo;
import software.sebastian.mondragon.battleship.game.service.GameService;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private static final Method PARSE_PORT_METHOD = resolveParsePort();

    @Test
    void testMainSeEjecutaSinErrores() {
        Logger logger = Logger.getLogger(Main.class.getName());
        CapturingHandler handler = new CapturingHandler();
        boolean originalUseParentHandlers = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        try {
            assertDoesNotThrow(() -> Main.main(new String[0]));
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalUseParentHandlers);
            logger.setLevel(originalLevel);
        }
        List<String> logs = handler.snapshot();
        handler.close();
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Demo finalizado")));
    }

    @Test
    void testDemoModeInvocadoDesdeArgumentos() throws Exception {
        List<String> logs = captureMainLogs(Level.INFO, () -> Main.main(new String[]{"demo"}));
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Demo finalizado")));
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
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        List<String> logs = captureMainLogs(Level.ALL, () -> {
            Thread serverThread = new Thread(() -> Main.main(new String[]{String.valueOf(port)}), "main-server-thread");
            serverThread.start();
            try {
                waitUntilPortOpen(port);
                waitUntilThreadWaiting(serverThread);
            } catch (Exception e) {
                serverThread.interrupt();
                throw new RuntimeException(e);
            }
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
        });

        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Servidor TCP escuchando")));
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Servidor interrumpido")));
    }

    @Test
    void testModoServerConPuertoPersonalizado() throws Exception {
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        List<String> logs = captureMainLogs(Level.ALL, () -> {
            Thread serverThread = new Thread(() -> Main.main(new String[]{"server", String.valueOf(port)}), "main-server-thread-server");
            serverThread.start();
            try {
                waitUntilPortOpen(port);
                waitUntilThreadWaiting(serverThread);
            } catch (Exception e) {
                serverThread.interrupt();
                throw new RuntimeException(e);
            }
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
        });

        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Servidor TCP escuchando")));
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Servidor interrumpido")));
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
    void testDeterminarOtroJugadorCubreAmbosResultados() {
        Jugador j1 = new Jugador(1);
        Jugador j2 = new Jugador(2);

        int otroCuandoTurnoEsJ1 = Main.determinarOtroJugador(j1.getId(), j1, j2);
        int otroCuandoTurnoEsJ2 = Main.determinarOtroJugador(j2.getId(), j1, j2);

        assertEquals(j2.getId(), otroCuandoTurnoEsJ1);
        assertEquals(j1.getId(), otroCuandoTurnoEsJ2);
    }

    @Test
    void testEjecutarDisparoRegistraError() {
        Logger logger = Logger.getLogger(Main.class.getName());
        CapturingHandler handler = new CapturingHandler();
        boolean originalUseParentHandlers = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        try {
            Main.ejecutarDisparo(new GameServiceQueFalla(), 1, 1, 0, 0);
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalUseParentHandlers);
            logger.setLevel(originalLevel);
        }
        List<String> logs = handler.snapshot();
        handler.close();
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Error en disparo")));
    }

    @Test
    void testConstructorCubreInicializacion() {
        assertNotNull(new Main());
    }

    @Test
    void testValidarMapasExitoso() {
        Mapa mapa1 = new Mapa(1, 2, 2);
        Mapa mapa2 = new Mapa(2, 2, 2);
        assertDoesNotThrow(() -> Main.validarMapas(mapa1, mapa2));
    }

    @Test
    void testValidarMapasLanzaExcepcion() {
        Mapa mapa = new Mapa(1, 2, 2);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Main.validarMapas(null, mapa));
        assertEquals("Mapas no inicializados", ex.getMessage());
    }

    @Test
    void testValidarMapasLanzaExcepcionCuandoSegundoEsNull() {
        Mapa mapa = new Mapa(1, 2, 2);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Main.validarMapas(mapa, null));
        assertEquals("Mapas no inicializados", ex.getMessage());
    }

    @Test
    void testModoPuertoDirectoInvalidoRegistraErrorEspecifico() throws Exception {
        List<String> logs = captureMainLogs(Level.SEVERE, () -> Main.main(new String[]{"no-es-numero"}));
        assertTrue(logs.stream().anyMatch(msg -> msg.contains("Entrada inv")));
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

    private static final class GameServiceQueFalla extends GameService {
        GameServiceQueFalla() {
            super(new InMemoryRepo(), (id, msg) -> { });
        }

        @Override
        public ResultadoDisparo disparar(int jugadorId, int partidoId, int fila, int columna) {
            throw new IllegalStateException("fallo forzado");
        }
    }
}
