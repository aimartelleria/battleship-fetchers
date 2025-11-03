package software.sebastian.mondragon.battleship.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Mapa {
    private final int id;
    private final int rows;
    private final int cols;
    // mapa de coordenadaId -> Coordenada
    private final Map<Integer, Coordenada> coordenadas = new HashMap<>();
    private final Map<Integer, Barco> barcos = new HashMap<>();
    private final AtomicInteger coordIdGen = new AtomicInteger(1);
    private final AtomicInteger barcoIdGen = new AtomicInteger(1);

    public Mapa(int id, int rows, int cols) {
        this.id = id;
        this.rows = rows;
        this.cols = cols;
        initCoordenadas();
    }

    private void initCoordenadas() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int coordId = coordIdGen.getAndIncrement();
                coordenadas.put(coordId, new Coordenada(coordId, r, c));
            }
        }
    }

    public int getId() { return id; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

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
        if (posiciones == null || posiciones.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos una coordenada");
        }

        List<Coordenada> coords = new ArrayList<>();
        Set<String> usados = new HashSet<>();

        for (int[] pos : posiciones) {
            if (pos == null || pos.length != 2) {
                throw new IllegalArgumentException("Coordenada inválida (esperado [fila,columna])");
            }
            int fila = pos[0];
            int col = pos[1];
            String clave = fila + ":" + col;
            if (!usados.add(clave)) {
                throw new IllegalArgumentException("Coordenada duplicada: " + fila + "," + col);
            }
            Optional<Coordenada> oc = buscarPorFilaCol(fila, col);
            if (!oc.isPresent()) {
                throw new IllegalArgumentException("Posicion fuera de mapa: " + fila + "," + col);
            }
            Coordenada coord = oc.get();
            if (coord.getBarcoId() != null) {
                throw new IllegalArgumentException("Ya existe un barco en " + fila + "," + col);
            }
            coords.add(coord);
        }

        if (coords.size() > 1) {
            validarAlineacionYContiguedad(coords);
        }

        int barcoId = barcoIdGen.getAndIncrement();
        Barco barco = new Barco(barcoId);
        for (Coordenada coord : coords) {
            coord.setBarcoId(barcoId);
            barco.addCoordenada(coord.getId());
        }
        barcos.put(barcoId, barco);
        return barco;
    }

    private void validarAlineacionYContiguedad(List<Coordenada> coords) {
        Set<Integer> filas = coords.stream().map(Coordenada::getFila).collect(Collectors.toSet());
        Set<Integer> columnas = coords.stream().map(Coordenada::getColumna).collect(Collectors.toSet());

        if (filas.size() == 1) {
            List<Integer> ordenCols = columnas.stream().sorted().toList();
            verificarConsecutivos(ordenCols, "columnas");
        } else if (columnas.size() == 1) {
            List<Integer> ordenFilas = filas.stream().sorted().toList();
            verificarConsecutivos(ordenFilas, "filas");
        } else {
            throw new IllegalArgumentException("El barco debe colocarse en línea recta horizontal o vertical");
        }
    }

    private void verificarConsecutivos(List<Integer> valoresOrdenados, String tipo) {
        for (int i = 1; i < valoresOrdenados.size(); i++) {
            int anterior = valoresOrdenados.get(i - 1);
            int actual = valoresOrdenados.get(i);
            if (actual != anterior + 1) {
                throw new IllegalArgumentException("Las " + tipo + " del barco deben ser contiguas");
            }
        }
    }

    public Barco getBarco(int id) {
        return barcos.get(id);
    }

    public Collection<Barco> getBarcos() {
        return Collections.unmodifiableCollection(barcos.values());
    }
}
