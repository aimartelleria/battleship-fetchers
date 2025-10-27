package software.sebastian.mondragon.battleship.model;

public class Coordenada {
    private final int id;
    private final int fila;
    private final int columna;
    private Integer barcoId; // null si no hay barco
    private EstadoCoordenada estado;

    public Coordenada(int id, int fila, int columna) {
        this.id = id;
        this.fila = fila;
        this.columna = columna;
        this.barcoId = null;
        this.estado = EstadoCoordenada.SIN_DISPARAR;
    }

    public int getId() { return id; }
    public int getFila() { return fila; }
    public int getColumna() { return columna; }
    public Integer getBarcoId() { return barcoId; }
    public void setBarcoId(Integer barcoId) { this.barcoId = barcoId; }
    public EstadoCoordenada getEstado() { return estado; }
    public void setEstado(EstadoCoordenada estado) { this.estado = estado; }

    @Override
    public String toString() {
        return String.format("Coordenada{id=%d, fila=%d, col=%d, barcoId=%s, estado=%s}",
                id, fila, columna, barcoId, estado);
    }
}
