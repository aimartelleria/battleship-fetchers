package software.sebastian.mondragon.battleship.repo;

import software.sebastian.mondragon.battleship.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepoTest {

    @Test
    void testCrearJugadorYMapaYPartido() {
        InMemoryRepo repo = new InMemoryRepo();
        Jugador j = repo.crearJugador();
        assertNotNull(repo.getJugador(j.getId()));

        Mapa m = repo.crearMapa(10, 10);
        assertNotNull(repo.getMapa(m.getId()));

        Partido p = repo.crearPartido();
        assertNotNull(repo.getPartido(p.getId()));
    }

    @Test
    void testGetEntidadInexistente() {
        InMemoryRepo repo = new InMemoryRepo();
        assertTrue(repo.getJugador(999) == null);
        assertTrue(repo.getMapa(999) == null);
        assertTrue(repo.getPartido(999) == null);
    }
}
