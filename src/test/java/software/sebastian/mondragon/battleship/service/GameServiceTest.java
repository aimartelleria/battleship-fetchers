package software.sebastian.mondragon.battleship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.model.*;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de GameService ajustado al InMemoryRepo actual.
 * No usa guardarJugador() ni métodos inexistentes.
 */
class GameServiceTest {

    private InMemoryRepo repo;
    private GameService service;

    @BeforeEach
    void setUp() {
        // repo en memoria real
        repo = new InMemoryRepo();
        // el segundo argumento puede ser un mock o lambda vacía para las notificaciones
        service = new GameService(repo, (id, msg) -> {});
    }

    @Test
    void testCrearPartidoYUnirse() {
        Jugador j1 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        assertNotNull(p);
        assertEquals(j1.getId(), p.getJugador1Id());

        Jugador j2 = service.crearJugador();
        service.unirsePartido(p.getId(), j2.getId());

        assertEquals(j2.getId(), p.getJugador2Id());
        assertEquals(EstadoPartido.EN_CURSO, p.getEstado());
    }

    @Test
    void testUnirseAPartidaLlenaLanzaExcepcion() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Jugador j3 = service.crearJugador();

        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        assertThrows(IllegalStateException.class, () ->
                service.unirsePartido(p.getId(), j3.getId()));
    }

    @Test
    void testUnirseAPartidaInexistenteLanzaExcepcion() {
        Jugador j = service.crearJugador();
        assertThrows(IllegalArgumentException.class, () ->
                service.unirsePartido(999, j.getId()));
    }

    @Test
    void testCrearPartidoJugadorInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearPartido(-1));
    }

    @Test
    void testDisparoFueraDeTurnoLanzaExcepcion() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        assertThrows(IllegalStateException.class, () ->
                service.disparar(j2.getId(), p.getId(), 1, 1));
    }

    @Test
    void testDisparoEnPartidaNoIniciadaLanzaExcepcion() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        p.setEstado(EstadoPartido.ESPERANDO_JUGADORES);
        assertThrows(IllegalStateException.class, () ->
                service.disparar(j1.getId(), p.getId(), 0, 0));
    }

    @Test
    void testDisparoAfueraDelMapaLanzaExcepcion() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();

        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        // el mapa por defecto del jugador probablemente tiene dimensiones 10x10
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        assertThrows(IllegalArgumentException.class, () ->
                service.disparar(j1.getId(), p.getId(), 999, 999));
    }

    @Test
    void testFlujoBasicoDeJuego() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();

        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        // colocar barcos en ambos jugadores
        service.colocarBarco(j1.getId(), Arrays.asList(new int[]{0, 0}, new int[]{0, 1}));
        service.colocarBarco(j2.getId(), Arrays.asList(new int[]{1, 0}, new int[]{1, 1}));

        // forzar inicio de partida
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        ResultadoDisparo res = service.disparar(j1.getId(), p.getId(), 1, 0);
        assertNotNull(res);
        assertTrue(List.of(ResultadoDisparo.AGUA, ResultadoDisparo.TOCADO, ResultadoDisparo.HUNDIDO)
                .contains(res));
    }
}
