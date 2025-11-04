package software.sebastian.mondragon.battleship.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

class BarcoTest {

    @Test
    void testCrearYActualizarBarco() {
        Barco barco = new Barco(1);
        barco.addCoordenada(10);
        barco.addCoordenada(11);

        assertEquals(2, barco.getCoordenadaIds().size());
        assertTrue(barco.getCoordenadaIds().containsAll(Arrays.asList(10, 11)));

        assertFalse(barco.isHundido());
        barco.setHundido(true);
        assertTrue(barco.isHundido());
        assertTrue(barco.toString().contains("Barco{"));
    }
}
