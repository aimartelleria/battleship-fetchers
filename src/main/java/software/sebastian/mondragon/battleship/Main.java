package software.sebastian.mondragon.battleship;

import software.sebastian.mondragon.battleship.model.*;
import software.sebastian.mondragon.battleship.repo.InMemoryRepo;
import software.sebastian.mondragon.battleship.service.GameService;
import software.sebastian.mondragon.battleship.service.GameService.Notifier;
import software.sebastian.mondragon.battleship.service.ResultadoDisparo;

import java.util.Arrays;

public class Main {
    static int determinarOtroJugador(int turno, Jugador j1, Jugador j2) {
        return (turno == j1.getId()) ? j2.getId() : j1.getId();
    }

    static void ejecutarDisparo(GameService gs, int jugadorId, int partidoId, int fila, int columna) {
        try {
            ResultadoDisparo resultado = gs.disparar(jugadorId, partidoId, fila, columna);
            System.out.println("Resultado: " + resultado);
        } catch (Exception ex) {
            System.out.println("Error en disparo: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        InMemoryRepo repo = new InMemoryRepo();

        // Notifier simple: imprime mensajes
        Notifier notifier = (jugadorId, mensaje) -> {
            System.out.printf("[Notificación -> Jugador %d] %s%n", jugadorId, mensaje);
        };

        GameService gs = new GameService(repo, notifier);

        // crear 2 jugadores
        Jugador j1 = gs.crearJugador();
        Jugador j2 = gs.crearJugador();
        System.out.println("Jugadores creados: " + j1 + ", " + j2);

        // crear partida por j1
        Partido p = gs.crearPartido(j1.getId());
        System.out.println("Partida creada: " + p);

        // j2 se une
        gs.unirsePartido(p.getId(), j2.getId());

        // Obtenemos mapas de los jugadores
        Mapa mapa1 = repo.getMapa(j1.getMapaId());
        Mapa mapa2 = repo.getMapa(j2.getMapaId());
        // evitar warnings de compilación por variables no usadas en esta demo
        if (mapa1 == null || mapa2 == null) {
            throw new IllegalStateException("Mapas no inicializados");
        }

        // Colocar barcos simples (por demo). Un barco de 2 celdas para cada uno.
        gs.colocarBarco(j1.getId(), Arrays.asList(new int[]{0,0}, new int[]{0,1}));
        gs.colocarBarco(j2.getId(), Arrays.asList(new int[]{5,5}, new int[]{5,6}));

        System.out.println("Barcos colocados. Empezamos a disparar...");

        // Suponiendo que p.getTurnoJugadorId() fue asignado
        int turno = repo.getPartido(p.getId()).getTurnoJugadorId();
        int other = determinarOtroJugador(turno, j1, j2);

        // Pequeño bucle demo: cada jugador dispara a una coordenada fija (solo demo)
        ejecutarDisparo(gs, turno, p.getId(), 5, 5); // si toca barco de j2
        ejecutarDisparo(gs, other, p.getId(), 0, 0); // turno alterno

        // continuar hasta que uno gane (en una app real, aquí habría loops / UI / REST)
        System.out.println("Demo finalizado. Estado final del partido: " + repo.getPartido(p.getId()));
    }
}
