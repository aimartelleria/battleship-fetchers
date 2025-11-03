package software.sebastian.mondragon.battleship.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PartidoTest {

    @Test
    void testPropiedadesPartido() {
        Partido partido = new Partido(0);
        assertEquals(EstadoPartido.ESPERANDO_JUGADORES, partido.getEstado());

        partido.setJugador1Id(10);
        partido.setJugador2Id(20);
        partido.setTurnoJugadorId(10);
        partido.setEstado(EstadoPartido.EN_CURSO);

        assertEquals(10, partido.getJugador1Id());
        assertEquals(20, partido.getJugador2Id());
        assertEquals(10, partido.getTurnoJugadorId());
        assertEquals(EstadoPartido.EN_CURSO, partido.getEstado());
    }

    @Test
    void testOtroJugadorDevuelveJugador2CuandoConsultaJugador1() {
        Partido partido = new Partido(1);
        partido.setJugador1Id(10);
        partido.setJugador2Id(20);

        assertTrue(partido.otroJugador(10).isPresent());
        assertEquals(20, partido.otroJugador(10).orElseThrow());
    }

    @Test
    void testOtroJugadorDevuelveJugador1CuandoConsultaJugador2() {
        Partido partido = new Partido(2);
        partido.setJugador1Id(10);
        partido.setJugador2Id(20);

        assertTrue(partido.otroJugador(20).isPresent());
        assertEquals(10, partido.otroJugador(20).orElseThrow());
    }

    @Test
    void testOtroJugadorVacioCuandoFaltaSegundoJugador() {
        Partido partido = new Partido(3);
        partido.setJugador1Id(10);

        assertTrue(partido.otroJugador(10).isEmpty());
    }

    @Test
    void testOtroJugadorVacioCuandoFaltaPrimerJugador() {
        Partido partido = new Partido(6);
        partido.setJugador2Id(20);

        assertTrue(partido.otroJugador(20).isEmpty());
    }

    @Test
    void testOtroJugadorVacioCuandoIdNoCoincide() {
        Partido partido = new Partido(4);
        partido.setJugador1Id(10);
        partido.setJugador2Id(20);

        assertTrue(partido.otroJugador(99).isEmpty());
    }

    @Test
    void testOtroJugadorVacioCuandoNoHayJugadores() {
        Partido partido = new Partido(5);
        assertTrue(partido.otroJugador(1).isEmpty());
    }
}
