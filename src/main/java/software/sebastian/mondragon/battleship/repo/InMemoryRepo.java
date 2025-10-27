package software.sebastian.mondragon.battleship.repo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import software.sebastian.mondragon.battleship.model.Jugador;
import software.sebastian.mondragon.battleship.model.Mapa;
import software.sebastian.mondragon.battleship.model.Partido;

public class InMemoryRepo {
    private final AtomicInteger partidoGen = new AtomicInteger(1);
    private final AtomicInteger jugadorGen = new AtomicInteger(1);
    private final AtomicInteger mapaGen = new AtomicInteger(1);

    private final Map<Integer, Partido> partidos = new ConcurrentHashMap<>();
    private final Map<Integer, Jugador> jugadores = new ConcurrentHashMap<>();
    private final Map<Integer, Mapa> mapas = new ConcurrentHashMap<>();

    /* Jugadores */
    public Jugador crearJugador() {
        int id = jugadorGen.getAndIncrement();
        Jugador j = new Jugador(id);
        jugadores.put(id, j);
        return j;
    }
    public Jugador getJugador(int id) { return jugadores.get(id); }

    /* Mapa */
    public Mapa crearMapa(int filas, int columnas) {
        int id = mapaGen.getAndIncrement();
        Mapa m = new Mapa(id, filas, columnas);
        mapas.put(id, m);
        return m;
    }
    public Mapa getMapa(int id) { return mapas.get(id); }

    /* Partido */
    public Partido crearPartido() {
        int id = partidoGen.getAndIncrement();
        Partido p = new Partido(id);
        partidos.put(id, p);
        return p;
    }
    public Partido getPartido(int id) { return partidos.get(id); }

    public Collection<Partido> getTodosPartidos() { return partidos.values(); }
}

