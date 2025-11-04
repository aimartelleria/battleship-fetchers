package software.sebastian.mondragon.battleship.game.model;

import java.util.ArrayList;
import java.util.List;

public class Barco {
    private final int id;
    private final List<Integer> coordenadaIds; // ids de Coordenada
    private boolean hundido;

    public Barco(int id) {
        this.id = id;
        this.coordenadaIds = new ArrayList<>();
        this.hundido = false;
    }

    public int getId() { return id; }
    public List<Integer> getCoordenadaIds() { return coordenadaIds; }
    public void addCoordenada(int coordId) { coordenadaIds.add(coordId); }
    public boolean isHundido() { return hundido; }
    public void setHundido(boolean hundido) { this.hundido = hundido; }

    @Override
    public String toString() {
        return "Barco{" + "id=" + id + ", coordenadas=" + coordenadaIds + ", hundido=" + hundido + '}';
    }
}
