package software.sebastian.mondragon.battleship.service;


import software.sebastian.mondragon.battleship.model.*;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class GameService {
    private final InMemoryRepo repo;
    private final Random rnd = new Random();

    // Proveedor de notificaciones (puede reemplazarse por websockets / eventos)
    public interface Notifier {
        void notifyJugador(int jugadorId, String mensaje);
    }

    private final Notifier notifier;

    public GameService(InMemoryRepo repo, Notifier notifier) {
        this.repo = repo;
        this.notifier = notifier;
    }

    /* operaciones básicas */
    public Jugador crearJugador() {
        return repo.crearJugador();
    }

    public Partido crearPartido(int creadorJugadorId) {
        Jugador j = repo.getJugador(creadorJugadorId);
        if (j == null) throw new IllegalArgumentException("Jugador no existe: " + creadorJugadorId);

        Partido p = repo.crearPartido();
        p.setJugador1Id(creadorJugadorId);
        p.setEstado(EstadoPartido.ESPERANDO_JUGADORES);
        notifier.notifyJugador(creadorJugadorId, "Partida creada con id " + p.getId());
        return p;
    }

    public Partido unirsePartido(int partidoId, int jugadorId) {
        Partido p = repo.getPartido(partidoId);
        if (p == null) throw new IllegalArgumentException("Partido no existe: " + partidoId);
        if (p.getJugador2Id() != null) throw new IllegalStateException("Partido ya tiene 2 jugadores");
        if (p.getJugador1Id() != null && p.getJugador1Id() == jugadorId)
            throw new IllegalArgumentException("Jugador ya está en la partida");

        p.setJugador2Id(jugadorId);

        // Crear mapas por defecto para cada jugador si no tienen
        Jugador j = repo.getJugador(jugadorId);
        if (j.getMapaId() == null) {
            Mapa m = repo.crearMapa(10, 10);
            j.setMapaId(m.getId());
        }

        // también aseguramos que el jugador1 tenga mapa
        Jugador j1 = repo.getJugador(p.getJugador1Id());
        if (j1.getMapaId() == null) {
            Mapa m1 = repo.crearMapa(10, 10);
            j1.setMapaId(m1.getId());
        }

        // Cuando hay 2 jugadores se inicia automáticamente la partida
        iniciarPartidoSiListo(p);
        notifier.notifyJugador(jugadorId, "Te has unido a la partida " + p.getId());
        notifier.notifyJugador(p.getJugador1Id(), "Jugador " + jugadorId + " se ha unido a tu partida " + p.getId());
        return p;
    }

    private void iniciarPartidoSiListo(Partido p) {
        if (p.getJugador1Id() != null && p.getJugador2Id() != null && p.getEstado() == EstadoPartido.ESPERANDO_JUGADORES) {
            p.setEstado(EstadoPartido.EN_CURSO);
            // elegir aleatoriamente quien empieza
            int primero = rnd.nextBoolean() ? p.getJugador1Id() : p.getJugador2Id();
            p.setTurnoJugadorId(primero);
            notifier.notifyJugador(p.getJugador1Id(), "Partida " + p.getId() + " iniciada. Turno de: " + primero);
            notifier.notifyJugador(p.getJugador2Id(), "Partida " + p.getId() + " iniciada. Turno de: " + primero);
        }
    }

    /* colocar barco de forma manual: posiciones como lista de [fila,columna] */
    public Barco colocarBarco(int jugadorId, List<int[]> posiciones) {
        Jugador j = repo.getJugador(jugadorId);
        if (j == null) throw new IllegalArgumentException("Jugador no existe: " + jugadorId);
        if (j.getMapaId() == null) throw new IllegalStateException("Jugador no tiene mapa");
        Mapa mapa = repo.getMapa(j.getMapaId());
        return mapa.crearBarco(posiciones);
    }

    /* disparar */
    public ResultadoDisparo disparar(int jugadorId, int partidoId, int fila, int columna) {
        Partido p = repo.getPartido(partidoId);
        if (p == null) throw new IllegalArgumentException("Partido no existe");
        if (p.getEstado() != EstadoPartido.EN_CURSO) throw new IllegalStateException("Partida no en curso");
        if (!jugadorTieneTurno(jugadorId, p)) throw new IllegalStateException("No es tu turno");

        Optional<Integer> oponenteOp = p.otroJugador(jugadorId);
        if (!oponenteOp.isPresent()) throw new IllegalStateException("No hay oponente");

        int oponenteId = oponenteOp.get();
        Jugador opp = repo.getJugador(oponenteId);
        Mapa mapaOpp = repo.getMapa(opp.getMapaId());

        // buscar coordenada
        Optional<Coordenada> oc = mapaOpp.buscarPorFilaCol(fila, columna);
        if (!oc.isPresent()) {
            throw new IllegalArgumentException("Coordenada fuera del mapa");
        }
        Coordenada coord = oc.get();

        // si ya dispararon ahí
        if (coord.getEstado() != EstadoCoordenada.SIN_DISPARAR) {
            throw new IllegalStateException("Ya se ha disparado en esa coordenada");
        }

        ResultadoDisparo resultado;
        if (coord.getBarcoId() == null) {
            coord.setEstado(EstadoCoordenada.AGUA);
            resultado = ResultadoDisparo.AGUA;
            notifier.notifyJugador(jugadorId, "Disparaste a (" + fila + "," + columna + "): AGUA");
            notifier.notifyJugador(oponenteId, "Te han disparado en (" + fila + "," + columna + "): AGUA");
            // cambia turno
            cambiarTurno(p);
        } else {
            // hay barco -> tocado o hundido
            coord.setEstado(EstadoCoordenada.TOCADO); // provisional
            Barco barco = mapaOpp.getBarco(coord.getBarcoId());
            // comprobar si todas las coordenadas del barco están tocadas
            boolean hundido = barco.getCoordenadaIds().stream()
                    .map(mapaOpp::getCoordenadaById)
                    .allMatch(c -> c.getEstado() == EstadoCoordenada.TOCADO || c.getEstado() == EstadoCoordenada.HUNDIDO);

            if (hundido) {
                // marcar todas como hundidas
                barco.getCoordenadaIds().forEach(cid -> {
                    Coordenada cc = mapaOpp.getCoordenadaById(cid);
                    cc.setEstado(EstadoCoordenada.HUNDIDO);
                });
                barco.setHundido(true);
                resultado = ResultadoDisparo.HUNDIDO;
                notifier.notifyJugador(jugadorId, "Disparaste a (" + fila + "," + columna + "): HUNDIDO (barco id " + barco.getId() + ")");
                notifier.notifyJugador(oponenteId, "Te han disparado en (" + fila + "," + columna + "): HUNDIDO (barco id " + barco.getId() + ")");
            } else {
                coord.setEstado(EstadoCoordenada.TOCADO);
                resultado = ResultadoDisparo.TOCADO;
                notifier.notifyJugador(jugadorId, "Disparaste a (" + fila + "," + columna + "): TOCADO");
                notifier.notifyJugador(oponenteId, "Te han disparado en (" + fila + "," + columna + "): TOCADO");
                // en la mayoría de variantes, cuando tocas no cambia el turno (pero esto depende de la regla).
                // Aquí vamos a cambiar el turno también para hacer alternancia simple:
                cambiarTurno(p);
            }
        }

        // comprobar victoria: si todos los barcos del oponente están hundidos -> fin
        boolean todosHundidos = mapaOpp.getBarcos().stream().allMatch(Barco::isHundido);
        if (todosHundidos) {
            p.setEstado(EstadoPartido.FINALIZADO);
            notifier.notifyJugador(jugadorId, "¡Victoria! Has hundido todos los barcos del oponente.");
            notifier.notifyJugador(oponenteId, "Derrota. Todos tus barcos han sido hundidos.");
        }

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
}

