package software.sebastian.mondragon.battleship.game.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapaTest {

    private Mapa crearMapa() {
        return new Mapa(1, 10, 10);
    }

    @Test
    void crearBarcoHorizontalContiguoEsValido() {
        Mapa mapa = crearMapa();
        List<int[]> posiciones = Arrays.asList(
                new int[]{0, 0},
                new int[]{0, 1},
                new int[]{0, 2}
        );

        assertDoesNotThrow(() -> mapa.crearBarco(posiciones));
    }

    @Test
    void crearBarcoVerticalContiguoEsValido() {
        Mapa mapa = crearMapa();
        List<int[]> posiciones = Arrays.asList(
                new int[]{1, 5},
                new int[]{2, 5},
                new int[]{3, 5}
        );

        assertDoesNotThrow(() -> mapa.crearBarco(posiciones));
    }

    @Test
    void crearBarcoDiagonalLanzaExcepcion() {
        Mapa mapa = crearMapa();
        List<int[]> posiciones = Arrays.asList(
                new int[]{0, 0},
                new int[]{1, 1}
        );

        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posiciones));
    }

    @Test
    void crearBarcoNoContiguoLanzaExcepcion() {
        Mapa mapa = crearMapa();
        List<int[]> posiciones = Arrays.asList(
                new int[]{4, 4},
                new int[]{4, 6}
        );

        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posiciones));
    }

    @Test
    void crearBarcoDuplicadoLanzaExcepcion() {
        Mapa mapa = crearMapa();
        List<int[]> posiciones = Arrays.asList(
                new int[]{2, 2},
                new int[]{2, 2}
        );

        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posiciones));
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

    @Test
    void crearBarcoConListaNullLanzaExcepcion() {
        Mapa mapa = crearMapa();
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(null));
    }

    @Test
    void crearBarcoConListaVaciaLanzaExcepcion() {
        Mapa mapa = crearMapa();
        List<int[]> vacia = List.of();
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(vacia));
    }

    @Test
    void crearBarcoConCoordenadaNullLanzaExcepcion() {
        Mapa mapa = crearMapa();
        List<int[]> posiciones = new ArrayList<>();
        posiciones.add(null);
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posiciones));
    }

    @Test
    void crearBarcoConCoordenadaMalFormadaLanzaExcepcion() {
        Mapa mapa = crearMapa();
        List<int[]> posiciones = Arrays.asList(new int[]{0});
        assertThrows(IllegalArgumentException.class, () -> mapa.crearBarco(posiciones));
    }

    @Test
    void gettersDevuelvenDimensionesConfiguradas() {
        Mapa mapa = new Mapa(42, 7, 9);
        assertEquals(7, mapa.getRows());
        assertEquals(9, mapa.getCols());
    }

    @Test
    void getTodasCoordenadasReturnsUnmodifiableCollection() {
        Mapa mapa = new Mapa(1, 2, 3);

        Collection<Coordenada> todas = mapa.getTodasCoordenadas();

        assertEquals(6, todas.size(), "Debe haber una coordenada por cada celda del mapa");

        Coordenada first = todas.iterator().next();
        assertThrows(UnsupportedOperationException.class, () -> todas.add(first),
                "La colección devuelta por getTodasCoordenadas debe ser inmodificable");
    }
}
