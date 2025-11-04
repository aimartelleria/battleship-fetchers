package software.sebastian.mondragon.battleship.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JugadorTest {

    @Test
    void testJugadorMapaAsignacion() {
        Jugador j = new Jugador(1);
        assertNull(j.getMapaId());

        j.setMapaId(5);
        assertEquals(5, j.getMapaId());
    }
}
