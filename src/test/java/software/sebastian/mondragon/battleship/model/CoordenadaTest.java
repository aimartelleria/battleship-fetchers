package software.sebastian.mondragon.battleship.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordenadaTest {

    @Test
    void testInicializacionYEstado() {
        Coordenada c = new Coordenada(1, 2, 3);
        assertEquals(1, c.getId());
        assertEquals(2, c.getFila());
        assertEquals(3, c.getColumna());
        assertNull(c.getBarcoId());
        assertEquals(EstadoCoordenada.SIN_DISPARAR, c.getEstado());

        c.setBarcoId(10);
        c.setEstado(EstadoCoordenada.TOCADO);
        assertEquals(10, c.getBarcoId());
        assertEquals(EstadoCoordenada.TOCADO, c.getEstado());
    }
}
