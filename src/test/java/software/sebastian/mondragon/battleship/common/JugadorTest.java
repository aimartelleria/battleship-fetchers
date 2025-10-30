package software.sebastian.mondragon.battleship.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JugadorTest {

    @Test
    void testInstanciacion() {
        Jugador jugador = new Jugador();
        assertNotNull(jugador);
    }
}
