package software.sebastian.mondragon.battleship.game.service;

import software.sebastian.mondragon.battleship.game.model.*;
import software.sebastian.mondragon.battleship.game.repo.InMemoryRepo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GameService {
    private final InMemoryRepo repo;

    // Proveedor de notificaciones (puede reemplazarse por websockets / eventos)
    public interface Notifier {
        void notifyJugador(int jugadorId, String mensaje);
    }

    private final Notifier notifier;

    public GameService(InMemoryRepo repo, Notifier notifier) {
        this.repo = repo;
        this.notifier = notifier;
    }

    /* operaciones basicas */
    public Jugador crearJugador() {
        return repo.crearJugador();
    }

    public Partido crearPartido(int creadorJugadorId) {
        obtenerJugador(creadorJugadorId);
        Partido p = repo.crearPartido();
        p.setJugador1Id(creadorJugadorId);
        p.setEstado(EstadoPartido.ESPERANDO_JUGADORES);
        notifier.notifyJugador(creadorJugadorId, "Partida creada con id " + p.getId());
        return p;
    }

    public Partido unirsePartido(int partidoId, int jugadorId) {
        Partido p = obtenerPartido(partidoId, "Partido no existe: " + partidoId);
        if (p.getJugador2Id() != null) throw new IllegalStateException("Partido ya tiene 2 jugadores");
        if (Objects.equals(p.getJugador1Id(), jugadorId))
            throw new IllegalArgumentException("Jugador ya esta en la partida");

        p.setJugador2Id(jugadorId);

        // Crear mapas por defecto para cada jugador si no tienen
        Jugador j = obtenerJugador(jugadorId);
        asegurarMapaParaJugador(j);

        // tambien aseguramos que el jugador1 tenga mapa
        Jugador j1 = obtenerJugador(p.getJugador1Id());
        asegurarMapaParaJugador(j1);

        // Cuando hay 2 jugadores se inicia automaticamente la partida
        iniciarPartidoSiListo(p);
        notifier.notifyJugador(jugadorId, "Te has unido a la partida " + p.getId());
        notifier.notifyJugador(p.getJugador1Id(), "Jugador " + jugadorId + " se ha unido a tu partida " + p.getId());
        return p;
    }

    private void iniciarPartidoSiListo(Partido p) {
        if (p.getJugador1Id() != null && p.getJugador2Id() != null && p.getEstado() == EstadoPartido.ESPERANDO_JUGADORES) {
            p.setEstado(EstadoPartido.EN_CURSO);
            // elegir aleatoriamente quien empieza
            int primero = p.getJugador1Id();
            p.setTurnoJugadorId(primero);
            notifier.notifyJugador(p.getJugador1Id(), "Partida " + p.getId() + " iniciada. Turno de: " + primero);
            notifier.notifyJugador(p.getJugador2Id(), "Partida " + p.getId() + " iniciada. Turno de: " + primero);
        }
    }

    /* colocar barco de forma manual: posiciones como lista de [fila,columna] */
    public Barco colocarBarco(int jugadorId, List<int[]> posiciones) {
        Jugador j = obtenerJugador(jugadorId);
        Mapa mapa = obtenerMapaDeJugador(j);
        return mapa.crearBarco(posiciones);
    }

    /* disparar */
    public ResultadoDisparo disparar(int jugadorId, int partidoId, int fila, int columna) {
        Partido partido = obtenerPartidoEnCurso(partidoId);
        validarTurno(jugadorId, partido);
        int oponenteId = obtenerOponenteId(partido, jugadorId);
        Jugador oponente = obtenerJugador(oponenteId);
        Mapa mapaOponente = obtenerMapaDeJugador(oponente);

        Coordenada coordenada = obtenerCoordenada(mapaOponente, fila, columna);
        validarCoordenadaDisponible(coordenada);

        ResultadoDisparo resultado = coordenada.getBarcoId() == null
                ? procesarDisparoAgua(partido, jugadorId, oponenteId, fila, columna, coordenada)
                : procesarDisparoImpacto(partido, jugadorId, oponenteId, fila, columna, coordenada, mapaOponente);

        verificarFinPartido(partido, mapaOponente, jugadorId, oponenteId);
        return resultado;
    }

    private void cambiarTurno(Partido p) {
        Integer j1 = p.getJugador1Id();
        Integer j2 = p.getJugador2Id();
        if (j1 == null || j2 == null) return;
        p.setTurnoJugadorId(p.getTurnoJugadorId().equals(j1) ? j2 : j1);
        notifier.notifyJugador(p.getTurnoJugadorId(), "Es tu turno.");
    }

    private boolean jugadorTieneTurno(int jugadorId, Partido p) {
        return p.getTurnoJugadorId() != null && p.getTurnoJugadorId().equals(jugadorId);
    }

    private Jugador obtenerJugador(int jugadorId) {
        Jugador jugador = repo.getJugador(jugadorId);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no existe: " + jugadorId);
        }
        return jugador;
    }

    private Partido obtenerPartido(int partidoId, String mensajeSiNoExiste) {
        Partido partido = repo.getPartido(partidoId);
        if (partido == null) {
            throw new IllegalArgumentException(mensajeSiNoExiste);
        }
        return partido;
    }

    private Partido obtenerPartidoEnCurso(int partidoId) {
        Partido partido = obtenerPartido(partidoId, "Partido no existe");
        if (partido.getEstado() != EstadoPartido.EN_CURSO) {
            throw new IllegalStateException("Partida no en curso");
        }
        return partido;
    }

    private void validarTurno(int jugadorId, Partido partido) {
        if (!jugadorTieneTurno(jugadorId, partido)) {
            throw new IllegalStateException("No es tu turno");
        }
    }

    private int obtenerOponenteId(Partido partido, int jugadorId) {
        Optional<Integer> oponenteOp = partido.otroJugador(jugadorId);
        return oponenteOp.orElseThrow(() -> new IllegalStateException("No hay oponente"));
    }

    private void asegurarMapaParaJugador(Jugador jugador) {
        if (jugador.getMapaId() == null) {
            Mapa mapa = repo.crearMapa(10, 10);
            jugador.setMapaId(mapa.getId());
        }
    }

    private Mapa obtenerMapaDeJugador(Jugador jugador) {
        if (jugador.getMapaId() == null) {
            throw new IllegalStateException("Jugador no tiene mapa");
        }
        return repo.getMapa(jugador.getMapaId());
    }

    private Coordenada obtenerCoordenada(Mapa mapa, int fila, int columna) {
        return mapa.buscarPorFilaCol(fila, columna)
                .orElseThrow(() -> new IllegalArgumentException("Coordenada fuera del mapa"));
    }

    private void validarCoordenadaDisponible(Coordenada coordenada) {
        if (coordenada.getEstado() != EstadoCoordenada.SIN_DISPARAR) {
            throw new IllegalStateException("Ya se ha disparado en esa coordenada");
        }
    }

    private ResultadoDisparo procesarDisparoAgua(Partido partido, int atacanteId, int defensorId,
                                                 int fila, int columna, Coordenada coordenada) {
        coordenada.setEstado(EstadoCoordenada.AGUA);
        ResultadoDisparo resultado = ResultadoDisparo.AGUA;
        notificarResultadoDisparo(atacanteId, defensorId, fila, columna, resultado, null);
        cambiarTurno(partido);
        return resultado;
    }

    private ResultadoDisparo procesarDisparoImpacto(Partido partido, int atacanteId, int defensorId,
                                                    int fila, int columna, Coordenada coordenada, Mapa mapaOponente) {
        coordenada.setEstado(EstadoCoordenada.TOCADO);
        Barco barco = mapaOponente.getBarco(coordenada.getBarcoId());
        boolean hundido = barco.getCoordenadaIds().stream()
                .map(mapaOponente::getCoordenadaById)
                .allMatch(c -> c.getEstado() == EstadoCoordenada.TOCADO || c.getEstado() == EstadoCoordenada.HUNDIDO);

        if (hundido) {
            barco.getCoordenadaIds().forEach(cid -> {
                Coordenada cc = mapaOponente.getCoordenadaById(cid);
                cc.setEstado(EstadoCoordenada.HUNDIDO);
            });
            barco.setHundido(true);
            ResultadoDisparo resultado = ResultadoDisparo.HUNDIDO;
            notificarResultadoDisparo(atacanteId, defensorId, fila, columna, resultado, barco.getId());
            return resultado;
        }

        ResultadoDisparo resultado = ResultadoDisparo.TOCADO;
        notificarResultadoDisparo(atacanteId, defensorId, fila, columna, resultado, null);
        cambiarTurno(partido);
        return resultado;
    }

    private void verificarFinPartido(Partido partido, Mapa mapaOponente, int atacanteId, int defensorId) {
        boolean todosHundidos = mapaOponente.getBarcos().stream().allMatch(Barco::isHundido);
        if (todosHundidos) {
            partido.setEstado(EstadoPartido.FINALIZADO);
            notifier.notifyJugador(atacanteId, "Victoria! Has hundido todos los barcos del oponente.");
            notifier.notifyJugador(defensorId, "Derrota. Todos tus barcos han sido hundidos.");
        }
    }

    private void notificarResultadoDisparo(int atacanteId, int defensorId, int fila, int columna,
                                           ResultadoDisparo resultado, Integer barcoId) {
        String detalle = resultado.name();
        if (resultado == ResultadoDisparo.HUNDIDO && barcoId != null) {
            detalle = "HUNDIDO (barco id " + barcoId + ")";
        }
        notifier.notifyJugador(atacanteId, "Disparaste a (" + fila + "," + columna + "): " + detalle);
        notifier.notifyJugador(defensorId, "Te han disparado en (" + fila + "," + columna + "): " + detalle);
    }
}
