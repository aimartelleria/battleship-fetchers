package software.sebastian.mondragon.battleship.service;

import software.sebastian.mondragon.battleship.model.*;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private InMemoryRepo repo;
    private GameService service;

    @BeforeEach
    void setup() {
        repo = new InMemoryRepo();
        service = new GameService(repo, (id, msg) -> {}); // notifier vacío
    }

    @Test
    void testFlujoBasicoDePartida() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        // asegurar que ambos tienen mapa
        assertNotNull(repo.getJugador(j1.getId()).getMapaId());
        assertNotNull(repo.getJugador(j2.getId()).getMapaId());

        // colocar barcos
        service.colocarBarco(j1.getId(), Arrays.asList(new int[]{0,0}, new int[]{0,1}));
        service.colocarBarco(j2.getId(), Arrays.asList(new int[]{5,5}, new int[]{5,6}));

        // forzar inicio de partida manualmente
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        // j1 dispara al barco de j2
        ResultadoDisparo res1 = service.disparar(j1.getId(), p.getId(), 5, 5);
        assertTrue(res1 == ResultadoDisparo.TOCADO || res1 == ResultadoDisparo.HUNDIDO);

        // turno pasa a j2
        assertEquals(j2.getId(), p.getTurnoJugadorId());
    }

    @Test
    void testDisparoAgua() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        ResultadoDisparo r = service.disparar(j1.getId(), p.getId(), 0, 0);
        assertEquals(ResultadoDisparo.AGUA, r);
    }

    @Test
    void testNoEsTuTurno() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        assertThrows(IllegalStateException.class, () ->
                service.disparar(j2.getId(), p.getId(), 1, 1));
    }
}
