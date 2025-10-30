package software.sebastian.mondragon.battleship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.sebastian.mondragon.battleship.model.*;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;

import java.lang.reflect.InvocationTargetException;
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
        service.colocarBarco(j1.getId(), Arrays.asList(new int[]{0, 0}, new int[]{0, 1}));
        service.colocarBarco(j2.getId(), Arrays.asList(new int[]{5, 5}, new int[]{5, 6}));

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

        Executable disparoFueraDeTurno = () -> service.disparar(j2.getId(), p.getId(), 1, 1);
        assertThrows(IllegalStateException.class, disparoFueraDeTurno);
    }

    @Test
    void testCrearPartidoJugadorInexistente() {
        Executable crearPartido = () -> service.crearPartido(999);
        assertThrows(IllegalArgumentException.class, crearPartido);
    }

    @Test
    void testUnirsePartidoNoExiste() {
        Jugador j = service.crearJugador();
        Executable unirsePartido = () -> service.unirsePartido(999, j.getId());
        assertThrows(IllegalArgumentException.class, unirsePartido);
    }

    @Test
    void testUnirsePartidoLleno() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Jugador j3 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        Executable unirTercerJugador = () -> service.unirsePartido(p.getId(), j3.getId());
        assertThrows(IllegalStateException.class, unirTercerJugador);
    }

    @Test
    void testUnirsePartidoConMismoJugador() {
        Jugador j1 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        Executable unirMismoJugador = () -> service.unirsePartido(p.getId(), j1.getId());
        assertThrows(IllegalArgumentException.class, unirMismoJugador);
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
        List<int[]> posiciones = List.of(new int[]{0, 0});
        Executable colocarBarco = () -> service.colocarBarco(123, posiciones);
        assertThrows(IllegalArgumentException.class, colocarBarco);
    }

    @Test
    void testColocarBarcoSinMapaAsignado() {
        Jugador j = service.crearJugador();
        List<int[]> posiciones = List.of(new int[]{0, 0});
        Executable colocarBarco = () -> service.colocarBarco(j.getId(), posiciones);
        assertThrows(IllegalStateException.class, colocarBarco);
    }

    @Test
    void testDispararPartidoInexistente() {
        Jugador j = service.crearJugador();
        Executable disparoInexistente = () -> service.disparar(j.getId(), 999, 0, 0);
        assertThrows(IllegalArgumentException.class, disparoInexistente);
    }

    @Test
    void testDispararPartidoNoEnCurso() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        p.setEstado(EstadoPartido.FINALIZADO);
        p.setTurnoJugadorId(j1.getId());
        Executable disparoFueraDeEstado = () -> service.disparar(j1.getId(), p.getId(), 0, 0);
        assertThrows(IllegalStateException.class, disparoFueraDeEstado);
    }

    @Test
    void testDispararSinOponente() {
        Jugador j1 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());
        Executable disparoSinOponente = () -> service.disparar(j1.getId(), p.getId(), 0, 0);
        assertThrows(IllegalStateException.class, disparoSinOponente);
    }

    @Test
    void testDispararCoordenadaFuera() {
        Jugador j1 = service.crearJugador();
        Jugador j2 = service.crearJugador();
        Partido p = service.crearPartido(j1.getId());
        service.unirsePartido(p.getId(), j2.getId());
        p.setEstado(EstadoPartido.EN_CURSO);
        p.setTurnoJugadorId(j1.getId());
        Executable disparoFueraDeMapa = () -> service.disparar(j1.getId(), p.getId(), 50, 50);
        assertThrows(IllegalArgumentException.class, disparoFueraDeMapa);
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
        Executable disparoRepetido = () -> service.disparar(j1.getId(), p.getId(), 0, 0);
        assertThrows(IllegalStateException.class, disparoRepetido);
    }

    @Test
    void testValidarCoordenadaDisponibleConEstadoNoPermitido() throws Exception {
        Method method = GameService.class.getDeclaredMethod("validarCoordenadaDisponible", Coordenada.class);
        method.setAccessible(true);
        Coordenada coordenada = new Coordenada(1, 0, 0);
        coordenada.setEstado(EstadoCoordenada.AGUA);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, coordenada));
        assertTrue(ex.getCause() instanceof IllegalStateException);
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
    void testCambiarTurnoConUnSoloJugadorNoCambiaTurno() {
        Partido p = new Partido(2);
        p.setJugador1Id(10);
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

    @Test
    void testIniciarPartidoConUnSoloJugadorMantieneEstado() {
        Partido p = new Partido(3);
        p.setJugador1Id(1);
        invokePrivate("iniciarPartidoSiListo", new Class[]{Partido.class}, p);
        assertEquals(EstadoPartido.ESPERANDO_JUGADORES, p.getEstado());
    }

    @Test
    void testIniciarPartidoConEstadoNoEsperandoNoCambia() {
        Partido p = new Partido(4);
        p.setJugador1Id(1);
        p.setJugador2Id(2);
        p.setEstado(EstadoPartido.EN_CURSO);
        invokePrivate("iniciarPartidoSiListo", new Class[]{Partido.class}, p);
        assertEquals(EstadoPartido.EN_CURSO, p.getEstado());
    }

    @Test
    void testJugadorTieneTurnoCoberturaCompleta() throws Exception {
        Method method = GameService.class.getDeclaredMethod("jugadorTieneTurno", int.class, Partido.class);
        method.setAccessible(true);

        Partido p = new Partido(5);
        assertFalse((Boolean) method.invoke(service, 1, p));

        p.setTurnoJugadorId(2);
        assertFalse((Boolean) method.invoke(service, 1, p));

        p.setTurnoJugadorId(1);
        assertTrue((Boolean) method.invoke(service, 1, p));
    }

    @Test
    void testAsegurarMapaParaJugadorNoReasigna() throws Exception {
        Method method = GameService.class.getDeclaredMethod("asegurarMapaParaJugador", Jugador.class);
        method.setAccessible(true);

        Jugador jugador = repo.crearJugador();
        Mapa mapaExistente = repo.crearMapa(3, 3);
        jugador.setMapaId(mapaExistente.getId());

        method.invoke(service, jugador);

        assertEquals(mapaExistente.getId(), jugador.getMapaId());
    }

    @Test
    void testProcesarDisparoImpactoConsideraCoordenadaHundida() throws Exception {
        Method method = GameService.class.getDeclaredMethod("procesarDisparoImpacto",
                Partido.class, int.class, int.class, int.class, int.class, Coordenada.class, Mapa.class);
        method.setAccessible(true);

        Partido partido = new Partido(6);
        partido.setJugador1Id(1);
        partido.setJugador2Id(2);
        partido.setTurnoJugadorId(1);

        Mapa mapa = repo.crearMapa(5, 5);
        Barco barco = mapa.crearBarco(List.of(new int[]{0, 0}, new int[]{0, 1}));
        Coordenada primera = mapa.getCoordenadaById(barco.getCoordenadaIds().get(0));
        Coordenada segunda = mapa.getCoordenadaById(barco.getCoordenadaIds().get(1));
        primera.setEstado(EstadoCoordenada.HUNDIDO);

        ResultadoDisparo resultado = (ResultadoDisparo) method.invoke(
                service, partido, 1, 2, segunda.getFila(), segunda.getColumna(), segunda, mapa);

        assertEquals(ResultadoDisparo.HUNDIDO, resultado);
    }

    @Test
    void testNotificarResultadoDisparoHundidoSinBarcoId() throws Exception {
        Method method = GameService.class.getDeclaredMethod("notificarResultadoDisparo",
                int.class, int.class, int.class, int.class, ResultadoDisparo.class, Integer.class);
        method.setAccessible(true);

        notifications.clear();
        method.invoke(service, 1, 2, 0, 0, ResultadoDisparo.HUNDIDO, null);

        assertTrue(notifications.stream().anyMatch(msg -> msg.contains("HUNDIDO")));
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
