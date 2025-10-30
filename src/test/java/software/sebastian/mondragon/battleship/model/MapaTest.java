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
        List<int[]> posiciones = List.of(new int[]{0, 0}, new int[]{0, 1});
        Barco b = mapa.crearBarco(posiciones);

        assertNotNull(b);
        assertEquals(2, b.getCoordenadaIds().size());
        assertTrue(mapa.getBarcos().contains(b));
    }

    @Test
    void testColocarBarcoFueraDelMapa() {
        Mapa mapa = new Mapa(1, 2, 2);
        List<int[]> posicionesFuera = List.of(new int[]{5, 5});
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posicionesFuera));
    }

    @Test
    void testColocarBarcoEnCeldaOcupada() {
        Mapa mapa = new Mapa(1, 3, 3);
        List<int[]> posicion = List.of(new int[]{0, 0});
        mapa.crearBarco(posicion);
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posicion));
    }
}
