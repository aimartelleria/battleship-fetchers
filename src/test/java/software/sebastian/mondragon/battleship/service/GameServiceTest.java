package software.sebastian.mondragon.battleship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.sebastian.mondragon.battleship.model.*;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private InMemoryRepo repo;
    private GameService service;
    private List<String> notifications;

    @BeforeEach
    void setup() {
        repo = new InMemoryRepo();
        notifications = new ArrayList<>();
        service = new GameService(repo, (id, msg) -> notifications.add(id + ":" + msg));
    }

    @Test
    void testFlujoBasicoDePartida() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        // asegurar que ambos tienen mapa
        assertNotNull(repo.getJugador(j1.getId()).getMapaId());
        assertNotNull(repo.getJugador(j2.getId()).getMapaId());

        // colocar barcos
        service.colocarBarco(j1.getId(), Arrays.asList(new int[]{0,0}, new int[]{0,1}));
        service.colocarBarco(j2.getId(), Arrays.asList(new int[]{5,5}, new int[]{5,6}));

        // forzar inicio de partida manualmente
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        // j1 dispara al barco de j2
        ResultadoDisparo res1 = service.disparar(j1.getId(), p.getId(), 5, 5);
        assertTrue(res1 == ResultadoDisparo.TOCADO || res1 == ResultadoDisparo.HUNDIDO);

        // turno pasa a j2
        assertEquals(j2.getId(), p.getTurnoJugadorId());
    }

    @Test
    void testDisparoAgua() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        ResultadoDisparo r = service.disparar(j1.getId(), p.getId(), 0, 0);
        assertEquals(ResultadoDisparo.AGUA, r);
        assertFalse(notifications.isEmpty());
    }

    @Test
    void testNoEsTuTurno() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        assertThrows(IllegalStateException.class, () ->
                service.disparar(j2.getId(), p.getId(), 1, 1));
    }

    @Test
    void testCrearPartidoJugadorInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.crearPartido(999));
    }

    @Test
    void testUnirsePartidoNoExiste() {
        Jugador j = service.crearJugador();
        assertThrows(IllegalArgumentException.class, () -> service.unirsePartido(999, j.getId()));
    }

    @Test
    void testUnirsePartidoLleno() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Jugador j3 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        assertThrows(IllegalStateException.class, () -> service.unirsePartido(p.getId(), j3.getId()));
    }

    @Test
    void testUnirsePartidoConMismoJugador() {
        Jugador j1 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        assertThrows(IllegalArgumentException.class, () -> service.unirsePartido(p.getId(), j1.getId()));
    }

    @Test
    void testUnirsePartidoIniciaJuego() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());

        assertEquals(EstadoPartido.EN_CURSO, repo.getPartido(p.getId()).getEstado());
        Integer turno = repo.getPartido(p.getId()).getTurnoJugadorId();
        assertNotNull(turno);
        assertTrue(List.of(j1.getId(), j2.getId()).contains(turno));
    }

    @Test
    void testColocarBarcoJugadorInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> service.colocarBarco(123, List.of(new int[]{0, 0})));
    }

    @Test
    void testColocarBarcoSinMapaAsignado() {
        Jugador j = service.crearJugador();
        assertThrows(IllegalStateException.class,
                () -> service.colocarBarco(j.getId(), List.of(new int[]{0, 0})));
    }

    @Test
    void testDispararPartidoInexistente() {
        Jugador j = service.crearJugador();
        assertThrows(IllegalArgumentException.class,
                () -> service.disparar(j.getId(), 999, 0, 0));
    }

    @Test
    void testDispararPartidoNoEnCurso() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        p.setEstado(EstadoPartido.FINALIZADO);
        p.setTurnoJugadorId(j1.getId());
        assertThrows(IllegalStateException.class,
                () -> service.disparar(j1.getId(), p.getId(), 0, 0));
    }

    @Test
    void testDispararSinOponente() {
        Jugador j1 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());
        assertThrows(IllegalStateException.class,
                () -> service.disparar(j1.getId(), p.getId(), 0, 0));
    }

    @Test
    void testDispararCoordenadaFuera() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());
        assertThrows(IllegalArgumentException.class,
                () -> service.disparar(j1.getId(), p.getId(), 50, 50));
    }

    @Test
    void testDispararCoordenadaRepetida() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());
        ResultadoDisparo r = service.disparar(j1.getId(), p.getId(), 0, 0);
        assertEquals(ResultadoDisparo.AGUA, r);
        p.setTurnoJugadorId(j1.getId());
        assertThrows(IllegalStateException.class,
                () -> service.disparar(j1.getId(), p.getId(), 0, 0));
    }

    @Test
    void testDispararHundidoYGanador() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        service.colocarBarco(j2.getId(), Arrays.asList(new int[]{0, 0}, new int[]{0, 1}));
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());

        ResultadoDisparo r1 = service.disparar(j1.getId(), p.getId(), 0, 0);
        assertEquals(ResultadoDisparo.TOCADO, r1);
        p.setTurnoJugadorId(j1.getId());
        ResultadoDisparo r2 = service.disparar(j1.getId(), p.getId(), 0, 1);
        assertEquals(ResultadoDisparo.HUNDIDO, r2);

        Partido actualizado = repo.getPartido(p.getId());
        assertEquals(EstadoPartido.FINALIZADO, actualizado.getEstado());

        Mapa mapa = repo.getMapa(repo.getJugador(j2.getId()).getMapaId());
        Optional<Coordenada> c1 = mapa.buscarPorFilaCol(0, 0);
        Optional<Coordenada> c2 = mapa.buscarPorFilaCol(0, 1);
        assertTrue(c1.isPresent() && c2.isPresent());
        assertEquals(EstadoCoordenada.HUNDIDO, c1.get().getEstado());
        assertEquals(EstadoCoordenada.HUNDIDO, c2.get().getEstado());
    }

    @Test
    void testCambiarTurnoSinJugadoresNoHaceNada() {
        Partido p = new Partido(1);
        invokePrivate("cambiarTurno", new Class[]{Partido.class}, p);
        assertNull(p.getTurnoJugadorId());
    }

    @Test
    void testIniciarPartidoNoListoMantieneEstado() {
        Partido p = new Partido(2);
        invokePrivate("iniciarPartidoSiListo", new Class[]{Partido.class}, p);
        assertEquals(EstadoPartido.ESPERANDO_JUGADORES, p.getEstado());
        assertNull(p.getTurnoJugadorId());
    }

    private void invokePrivate(String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method m = GameService.class.getDeclaredMethod(name, parameterTypes);
            m.setAccessible(true);
            m.invoke(service, args);
        } catch (Exception e) {
            fail(e);
        }
    }
}
