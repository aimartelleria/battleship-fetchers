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
        assertEquals(100, m.getTodasCoordenadas().size());

        Partido p = repo.crearPartido();
        assertNotNull(repo.getPartido(p.getId()));
    }
}
