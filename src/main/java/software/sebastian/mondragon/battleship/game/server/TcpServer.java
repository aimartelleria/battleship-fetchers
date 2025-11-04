package software.sebastian.mondragon.battleship.game.server;

import software.sebastian.mondragon.battleship.game.model.Barco;
import software.sebastian.mondragon.battleship.game.model.Jugador;
import software.sebastian.mondragon.battleship.game.model.Partido;
import software.sebastian.mondragon.battleship.game.repo.InMemoryRepo;
import software.sebastian.mondragon.battleship.game.service.GameService;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * Sencillo servidor TCP con protocolo basado en texto plano para interactuar con {@link GameService}.
 */
public class TcpServer {
    private static final Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

    private final int port;
    private final InMemoryRepo repo;
    private final Map<Integer, ClientHandler> clientsByPlayer = new ConcurrentHashMap<>();
    private final ExecutorService clientExecutor;
    private final GameService gameService;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public TcpServer(int port) {
        this.port = port;
        this.repo = new InMemoryRepo();
        this.clientExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "battleship-client");
            t.setDaemon(true);
            return t;
        });
        GameService.Notifier notifier = (jugadorId, mensaje) -> {
            ClientHandler handler = clientsByPlayer.get(jugadorId);
            if (handler != null) {
                handler.sendNotification(mensaje);
            }
        };
        this.gameService = new GameService(repo, notifier);
    }

    public void start() throws IOException {
        if (running) return;

        running = true;
        serverSocket = new ServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "battleship-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Intentionally ignored: server is stopping
            }
        }
        clientsByPlayer.values().forEach(ClientHandler::closeQuietly);
        clientExecutor.shutdownNow();
        if (acceptThread != null) {
            try {
                acceptThread.join(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
            }
        }
    }

    public GameService getGameService() {
        return gameService;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clientExecutor.submit(handler);
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "{0} [ERROR] Aceptando conexión: {1}",
                            new Object[]{timestamp(), e.getMessage()});
                }
            }
        }
    }

    private String timestamp() {
        return LocalDateTime.now().format(timeFormatter);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final Object sendLock = new Object();

        private volatile boolean active = true;
        private Integer playerId;

        private ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            sendLine("WELCOME Battleship TCP");
            sendLine("Type HELP for available commands.");
        }

        @Override
        public void run() {
            try (socket; reader; writer) {
                processClientCommands();
            } catch (IOException ex) {
                if (active) {
                    LOGGER.log(Level.WARNING, "{0} [WARN] Error en cliente: {1}",
                            new Object[]{timestamp(), ex.getMessage()});
                }
            } finally {
                cleanup();
            }
        }

        /**
         * Separated logic for reading and processing client commands.
         * Improves clarity and testability.
         */
        private void processClientCommands() throws IOException {
            String line;
            while (active && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    handleCommand(line);
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    sendError(ex.getMessage());
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Unexpected error processing command", ex);
                    sendError("Unexpected error: " + ex.getMessage());
                }
            }
        }

        private void handleCommand(String line) {
            String[] tokens = line.split("\\s+");
            String command = tokens[0].toUpperCase(Locale.ROOT);
            switch (command) {
                case "HELP" -> sendHelp();
                case "CREATE_PLAYER" -> crearJugador();
                case "USE_PLAYER" -> {
                    exigirArgs(tokens, 2);
                    asignarJugadorExistente(parseInt(tokens[1], "playerId"));
                }
                case "CREATE_GAME" -> {
                    exigirJugadorConectado();
                    crearPartido();
                }
                case "JOIN_GAME" -> {
                    exigirJugadorConectado();
                    exigirArgs(tokens, 2);
                    unirsePartido(parseInt(tokens[1], "gameId"));
                }
                case "LIST_GAMES" -> listarPartidos();
                case "PLACE_SHIP" -> {
                    exigirJugadorConectado();
                    exigirArgs(tokens, 2);
                    colocarBarco(tokens);
                }
                case "SHOOT" -> {
                    exigirJugadorConectado();
                    exigirArgs(tokens, 4);
                    disparar(tokens);
                }
                case "QUIT" -> {
                    sendLine("BYE");
                    active = false;
                }
                default -> sendError("Unknown command: " + command);
            }
        }

        private void sendHelp() {
            sendLine("COMMANDS:");
            sendLine("  CREATE_PLAYER               -> Crea un nuevo jugador y lo asocia a la sesión.");
            sendLine("  USE_PLAYER <playerId>       -> Usa un jugador existente.");
            sendLine("  CREATE_GAME                 -> Crea una partida con el jugador actual.");
            sendLine("  JOIN_GAME <gameId>          -> Une al jugador actual a la partida indicada.");
            sendLine("  LIST_GAMES                  -> Lista partidas existentes.");
            sendLine("  PLACE_SHIP <fila,col>...    -> Coloca un barco usando coordenadas pares.");
            sendLine("  SHOOT <gameId> <fila> <col> -> Realiza un disparo.");
            sendLine("  QUIT                        -> Cierra la conexión.");
        }

        private void crearJugador() {
            Jugador jugador = gameService.crearJugador();
            asociarJugador(jugador.getId());
            sendLine("PLAYER " + jugador.getId());
        }

        private void asignarJugadorExistente(int id) {
            Jugador jugador = repo.getJugador(id);
            if (jugador == null) {
                throw new IllegalArgumentException("Jugador no encontrado: " + id);
            }
            asociarJugador(jugador.getId());
            sendLine("PLAYER " + jugador.getId());
        }

        private void crearPartido() {
            Partido partido = gameService.crearPartido(playerId);
            sendLine("GAME " + partido.getId());
        }

        private void unirsePartido(int partidoId) {
            Partido partido = gameService.unirsePartido(partidoId, playerId);
            sendLine("JOINED " + partido.getId());
        }

        private void listarPartidos() {
            Collection<Partido> partidos = repo.getTodosPartidos();
            if (partidos.isEmpty()) {
                sendLine("GAMES");
                return;
            }
            String listado = partidos.stream()
                    .map(Partido::toString)
                    .collect(Collectors.joining(" | "));
            sendLine("GAMES " + listado);
        }

        private void colocarBarco(String[] tokens) {
            List<int[]> posiciones = new ArrayList<>();
            for (int i = 1; i < tokens.length; i++) {
                String[] partes = tokens[i].split(",");
                if (partes.length != 2) {
                    throw new IllegalArgumentException("Formato inválido de coordenada: " + tokens[i]);
                }
                int fila = parseInt(partes[0], "fila");
                int col = parseInt(partes[1], "columna");
                posiciones.add(new int[]{fila, col});
            }
            if (posiciones.isEmpty()) {
                throw new IllegalArgumentException("Debe especificar al menos una coordenada");
            }
            Barco barco = gameService.colocarBarco(playerId, posiciones);
            sendLine("SHIP " + barco.getId() + " SIZE " + barco.getCoordenadaIds().size());
        }

        private void disparar(String[] tokens) {
            if (tokens.length < 4) {
                throw new IllegalArgumentException("Uso: SHOOT <gameId> <fila> <col>");
            }
            int gameId = parseInt(tokens[1], "gameId");
            int fila = parseInt(tokens[2], "fila");
            int col = parseInt(tokens[3], "columna");
            ResultadoDisparo resultado = gameService.disparar(playerId, gameId, fila, col);
            sendLine("RESULT " + resultado.name());
        }

        private void asociarJugador(int nuevoJugadorId) {
            Integer anterior = this.playerId;
            this.playerId = nuevoJugadorId;
            clientsByPlayer.compute(nuevoJugadorId, (key, existing) -> {
                if (existing != null && existing != this) {
                    existing.sendNotification("Sesión reemplazada por una nueva conexión.");
                    existing.closeQuietly();
                }
                return this;
            });
            if (anterior != null && !Objects.equals(anterior, nuevoJugadorId)) {
                clientsByPlayer.remove(anterior, this);
            }
        }

        private void exigirJugadorConectado() {
            if (playerId == null) {
                throw new IllegalStateException("Debe crear o seleccionar un jugador primero.");
            }
        }

        private void exigirArgs(String[] tokens, int min) {
            if (tokens.length < min) {
                throw new IllegalArgumentException("Argumentos insuficientes para el comando.");
            }
        }

        private int parseInt(String value, String label) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Valor inválido para " + label + ": " + value);
            }
        }

        private void sendLine(String message) {
            synchronized (sendLock) {
                writer.println(message);
            }
        }

        private void sendError(String message) {
            sendLine("ERROR " + message);
        }

        void sendNotification(String message) {
            sendLine("NOTIFY " + message);
        }

        private void cleanup() {
            active = false;
            if (playerId != null) {
                clientsByPlayer.remove(playerId, this);
            }
        }

        void closeQuietly() {
            active = false;
            try {
                socket.close();
            } catch (IOException ignored) {
                // Intentionally ignored: cleanup on disconnect
            }
        }
    }
}
