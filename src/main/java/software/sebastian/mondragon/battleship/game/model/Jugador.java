package software.sebastian.mondragon.battleship.game.model;

public class Jugador {
    private final int id;
    private Integer mapaId; // id del mapa del jugador (cada jugador tiene su mapa)

    public Jugador(int id) {
        this.id = id;
        this.mapaId = null;
    }

    public int getId() { return id; }
    public Integer getMapaId() { return mapaId; }
    public void setMapaId(Integer mapaId) { this.mapaId = mapaId; }

    @Override
    public String toString() {
        return "Jugador{id=" + id + ", mapaId=" + mapaId + '}';
    }
}

