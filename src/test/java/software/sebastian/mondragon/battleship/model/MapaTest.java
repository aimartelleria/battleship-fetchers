package software.sebastian.mondragon.battleship.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MapaTest {

    @Test
    void testCrearMapaYCoordenadas() {
        Mapa mapa = new Mapa(1, 5, 5);
        assertEquals(25, mapa.getTodasCoordenadas().size());
        assertEquals(5, mapa.getFilas());
        assertEquals(5, mapa.getColumnas());

        Coordenada c = mapa.buscarPorFilaCol(2, 3).orElse(null);
        assertNotNull(c);
        assertEquals(2, c.getFila());
        assertEquals(3, c.getColumna());
    }

    @Test
    void testColocarBarcoCorrectamente() {
        Mapa mapa = new Mapa(1, 5, 5);
        Barco b = mapa.crearBarco(List.of(new int[]{0,0}, new int[]{0,1}));

        assertNotNull(b);
        assertEquals(2, b.getCoordenadaIds().size());
        assertTrue(mapa.getBarcos().contains(b));
    }

    @Test
    void testColocarBarcoFueraDelMapa() {
        Mapa mapa = new Mapa(1, 2, 2);
        assertThrows(IllegalArgumentException.class, () ->
                mapa.crearBarco(List.of(new int[]{5,5})));
    }

    @Test
    void testColocarBarcoEnCeldaOcupada() {
        Mapa mapa = new Mapa(1, 3, 3);
        mapa.crearBarco(List.of(new int[]{0,0}));
        assertThrows(IllegalArgumentException.class, () ->
                mapa.crearBarco(List.of(new int[]{0,0})));
    }
}
