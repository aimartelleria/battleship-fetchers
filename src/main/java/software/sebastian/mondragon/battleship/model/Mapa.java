package software.sebastian.mondragon.battleship.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Mapa {
    private final int id;
    private final int filas;
    private final int columnas;
    // mapa de coordenadaId -> Coordenada
    private final Map<Integer, Coordenada> coordenadas = new HashMap<>();
    private final Map<Integer, Barco> barcos = new HashMap<>();
    private final AtomicInteger coordIdGen = new AtomicInteger(1);
    private final AtomicInteger barcoIdGen = new AtomicInteger(1);

    public Mapa(int id, int filas, int columnas) {
        this.id = id;
        this.filas = filas;
        this.columnas = columnas;
        initCoordenadas();
    }

    private void initCoordenadas() {
        for (int r = 0; r < filas; r++) {
            for (int c = 0; c < columnas; c++) {
                int coordId = coordIdGen.getAndIncrement();
                coordenadas.put(coordId, new Coordenada(coordId, r, c));
            }
        }
    }

    public int getId() { return id; }
    public int getFilas() { return filas; }
    public int getColumnas() { return columnas; }

    public Optional<Coordenada> buscarPorFilaCol(int fila, int col) {
        return coordenadas.values().stream()
                .filter(cc -> cc.getFila() == fila && cc.getColumna() == col)
                .findFirst();
    }

    public Coordenada getCoordenadaById(int id) {
        return coordenadas.get(id);
    }

    public Collection<Coordenada> getTodasCoordenadas() {
        return Collections.unmodifiableCollection(coordenadas.values());
    }

    public Barco crearBarco(List<int[]> posiciones) {
        int barcoId = barcoIdGen.getAndIncrement();
        Barco barco = new Barco(barcoId);
        for (int[] pos : posiciones) {
            int fila = pos[0];
            int col = pos[1];
            Optional<Coordenada> oc = buscarPorFilaCol(fila, col);
            if (!oc.isPresent()) {
                throw new IllegalArgumentException("Posicion fuera de mapa: " + fila + "," + col);
            }
            Coordenada coord = oc.get();
            if (coord.getBarcoId() != null) {
                throw new IllegalArgumentException("Ya existe un barco en " + fila + "," + col);
            }
            coord.setBarcoId(barcoId);
            barco.addCoordenada(coord.getId());
        }
        barcos.put(barcoId, barco);
        return barco;
    }

    public Barco getBarco(int id) {
        return barcos.get(id);
    }

    public Collection<Barco> getBarcos() {
        return Collections.unmodifiableCollection(barcos.values());
    }
}
