package software.sebastian.mondragon.battleship.model;

import java.util.Optional;

public class Partido {
    private final int id;
    private Integer jugador1Id;
    private Integer jugador2Id;
    private Integer turnoJugadorId; // id del jugador que tiene el turno
    private EstadoPartido estado;

    public Partido(int id) {
        this.id = id;
        this.estado = EstadoPartido.ESPERANDO_JUGADORES;
    }

    public int getId() { return id; }
    public Integer getJugador1Id() { return jugador1Id; }
    public Integer getJugador2Id() { return jugador2Id; }
    public Integer getTurnoJugadorId() { return turnoJugadorId; }
    public EstadoPartido getEstado() { return estado; }

    public void setJugador1Id(Integer jugador1Id) { this.jugador1Id = jugador1Id; }
    public void setJugador2Id(Integer jugador2Id) { this.jugador2Id = jugador2Id; }
    public void setTurnoJugadorId(Integer turnoJugadorId) { this.turnoJugadorId = turnoJugadorId; }
    public void setEstado(EstadoPartido estado) { this.estado = estado; }

    public Optional<Integer> otroJugador(Integer jugadorId) {
        if (jugador1Id != null && jugador1Id.equals(jugadorId) && jugador2Id != null) return Optional.of(jugador2Id);
        if (jugador2Id != null && jugador2Id.equals(jugadorId) && jugador1Id != null) return Optional.of(jugador1Id);
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Partido{id=" + id + ", j1=" + jugador1Id + ", j2=" + jugador2Id + ", turno=" + turnoJugadorId + ", estado=" + estado + '}';
    }
}
