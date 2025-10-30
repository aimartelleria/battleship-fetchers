package software.sebastian.mondragon.battleship;

import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.model.Jugador;
import software.sebastian.mondragon.battleship.model.Mapa;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;
import software.sebastian.mondragon.battleship.service.GameService;
import software.sebastian.mondragon.battleship.service.ResultadoDisparo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

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

        boolean contains(String text) {
            return messages.stream().anyMatch(msg -> msg.contains(text));
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
