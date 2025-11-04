package software.sebastian.mondragon.battleship.game.repo;

import org.junit.jupiter.api.Test;

import software.sebastian.mondragon.battleship.game.model.*;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepoTest {

    @Test
    void testCrearJugadorYMapaYPartido() {
        InMemoryRepo repo = new InMemoryRepo();
        assertTrue(repo.getTodosPartidos().isEmpty());

        Jugador j = repo.crearJugador();
        assertNotNull(repo.getJugador(j.getId()));

        Mapa m = repo.crearMapa(10, 10);
        assertNotNull(repo.getMapa(m.getId()));

        Partido p = repo.crearPartido();
        assertNotNull(repo.getPartido(p.getId()));
        assertEquals(1, repo.getTodosPartidos().size());
    }

    @Test
    void testGetEntidadInexistente() {
        InMemoryRepo repo = new InMemoryRepo();
        assertNull(repo.getJugador(999));
        assertNull(repo.getMapa(999));
        assertNull(repo.getPartido(999));
    }
}
