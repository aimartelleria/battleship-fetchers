package software.sebastian.mondragon.battleship.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapaTest {

    private Mapa crearMapa() {
        return new Mapa(1, 10, 10);
    }

    @Test
    void crearBarcoHorizontalContiguoEsValido() {
        Mapa mapa = crearMapa();

        assertDoesNotThrow(() -> mapa.crearBarco(Arrays.asList(
                new int[]{0, 0},
                new int[]{0, 1},
                new int[]{0, 2}
        )));
    }

    @Test
    void crearBarcoVerticalContiguoEsValido() {
        Mapa mapa = crearMapa();

        assertDoesNotThrow(() -> mapa.crearBarco(Arrays.asList(
                new int[]{1, 5},
                new int[]{2, 5},
                new int[]{3, 5}
        )));
    }

    @Test
    void crearBarcoDiagonalLanzaExcepcion() {
        Mapa mapa = crearMapa();

        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(Arrays.asList(
                new int[]{0, 0},
                new int[]{1, 1}
        )));
    }

    @Test
    void crearBarcoNoContiguoLanzaExcepcion() {
        Mapa mapa = crearMapa();

        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(Arrays.asList(
                new int[]{4, 4},
                new int[]{4, 6}
        )));
    }

    @Test
    void crearBarcoDuplicadoLanzaExcepcion() {
        Mapa mapa = crearMapa();

        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(Arrays.asList(
                new int[]{2, 2},
                new int[]{2, 2}
        )));
    }

    @Test
    void crearBarcoFueraDelMapaLanzaExcepcion() {
        Mapa mapa = new Mapa(1, 2, 2);
        List<int[]> posicionesFuera = List.of(new int[]{5, 5});
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posicionesFuera));
    }

    @Test
    void crearBarcoEnCeldaOcupadaLanzaExcepcion() {
        Mapa mapa = new Mapa(1, 3, 3);
        List<int[]> posicion = List.of(new int[]{0, 0});
        mapa.crearBarco(posicion);
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posicion));
    }
}
