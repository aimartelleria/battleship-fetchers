package software.sebastian.mondragon.battleship.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import software.sebastian.mondragon.battleship.game.model.*;
import software.sebastian.mondragon.battleship.game.repo.InMemoryRepo;

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
        TestMatch match = prepareMatch().start();
        assertNotNull(repo.getJugador(match.hostId()).getMapaId());
        assertNotNull(repo.getJugador(match.guestId()).getMapaId());

        service.colocarBarco(match.hostId(), Arrays.asList(new int[]{0, 0}, new int[]{0, 1}));
        service.colocarBarco(match.guestId(), Arrays.asList(new int[]{5, 5}, new int[]{5, 6}));

        ResultadoDisparo res1 = service.disparar(match.hostId(), match.partidoId(), 5, 5);
        assertTrue(res1 == ResultadoDisparo.TOCADO || res1 == ResultadoDisparo.HUNDIDO);

        assertEquals(match.guestId(), match.partido().getTurnoJugadorId());
    }

    @Test
    void testDisparoAgua() {
        TestMatch match = prepareMatch().start();

        ResultadoDisparo r = service.disparar(match.hostId(), match.partidoId(), 0, 0);
        assertEquals(ResultadoDisparo.AGUA, r);
        assertFalse(notifications.isEmpty());
    }

    @Test
    void testNoEsTuTurno() {
        TestMatch match = prepareMatch().start();

        Executable disparoFueraDeTurno = () -> service.disparar(match.guestId(), match.partidoId(), 1, 1);
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
        TestMatch match = prepareMatch();
        Jugador j3 = service.crearJugador();
        Executable unirTercerJugador = () -> service.unirsePartido(match.partidoId(), j3.getId());
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
        TestMatch match = prepareMatch();
        Partido partido = repo.getPartido(match.partidoId());

        assertEquals(EstadoPartido.EN_CURSO, partido.getEstado());
        Integer turno = partido.getTurnoJugadorId();
        assertNotNull(turno);
        assertTrue(List.of(match.hostId(), match.guestId()).contains(turno));
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
        TestMatch match = prepareMatch();
        Partido p = match.partido();
        p.setEstado(EstadoPartido.FINALIZADO);
        p.setTurnoJugadorId(match.hostId());
        Executable disparoFueraDeEstado = () -> service.disparar(match.hostId(), match.partidoId(), 0, 0);
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
        TestMatch match = prepareMatch().start();
        Executable disparoFueraDeMapa = () -> service.disparar(match.hostId(), match.partidoId(), 50, 50);
        assertThrows(IllegalArgumentException.class, disparoFueraDeMapa);
    }

    @Test
    void testDispararCoordenadaRepetida() {
        TestMatch match = prepareMatch().start();
        ResultadoDisparo r = service.disparar(match.hostId(), match.partidoId(), 0, 0);
        assertEquals(ResultadoDisparo.AGUA, r);
        match.partido().setTurnoJugadorId(match.hostId());
        Executable disparoRepetido = () -> service.disparar(match.hostId(), match.partidoId(), 0, 0);
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
        TestMatch match = prepareMatch();
        service.colocarBarco(match.guestId(), Arrays.asList(new int[]{0, 0}, new int[]{0, 1}));
        match.start();

        ResultadoDisparo r1 = service.disparar(match.hostId(), match.partidoId(), 0, 0);
        assertEquals(ResultadoDisparo.TOCADO, r1);
        match.partido().setTurnoJugadorId(match.hostId());
        ResultadoDisparo r2 = service.disparar(match.hostId(), match.partidoId(), 0, 1);
        assertEquals(ResultadoDisparo.HUNDIDO, r2);

        Partido actualizado = repo.getPartido(match.partidoId());
        assertEquals(EstadoPartido.FINALIZADO, actualizado.getEstado());

        Mapa mapa = repo.getMapa(repo.getJugador(match.guestId()).getMapaId());
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

    private TestMatch prepareMatch() {
        return TestMatch.create(service);
    }

    private record TestMatch(Jugador host, Jugador guest, Partido partido) {
        static TestMatch create(GameService service) {
            Jugador host = service.crearJugador();
            Jugador guest = service.crearJugador();
            Partido partido = service.crearPartido(host.getId());
            service.unirsePartido(partido.getId(), guest.getId());
            return new TestMatch(host, guest, partido);
        }

        TestMatch start() {
            partido.setEstado(EstadoPartido.EN_CURSO);
            partido.setTurnoJugadorId(host.getId());
            return this;
        }

        int hostId() {
            return host.getId();
        }

        int guestId() {
            return guest.getId();
        }

        int partidoId() {
            return partido.getId();
        }
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
