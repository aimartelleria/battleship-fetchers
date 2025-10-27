package software.sebastian.mondragon.battleship.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartidoTest {

    @Test
    void testCreacionYEstados() {
        Partido p = new Partido(1);
        assertEquals(EstadoPartido.ESPERANDO_JUGADORES, p.getEstado());

        p.setJugador1Id(10);
        p.setJugador2Id(20);
        p.setTurnoJugadorId(10);
        p.setEstado(EstadoPartido.EN_CURSO);

        assertEquals(10, p.getJugador1Id());
        assertEquals(20, p.getJugador2Id());
        assertEquals(10, p.getTurnoJugadorId());
        assertEquals(EstadoPartido.EN_CURSO, p.getEstado());

        assertEquals(20, p.otroJugador(10).orElse(-1));
        assertEquals(10, p.otroJugador(20).orElse(-1));
    }
}
