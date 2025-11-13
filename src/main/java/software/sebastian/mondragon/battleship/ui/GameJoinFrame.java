package software.sebastian.mondragon.battleship.ui;

import software.sebastian.mondragon.battleship.game.client.ClientSession;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Logger;

// ----------------------------------
//  GAME JOIN FRAME
// ----------------------------------
@SuppressWarnings({"serial", "java:S110"})
public class GameJoinFrame extends BaseSessionFrame {
    private static final Logger LOGGER = Logger.getLogger(GameJoinFrame.class.getName());

    private final JTextField gameCodeField;
    private final JLabel statusLabel;
    final JTextArea infoArea;
    private final JTextArea notificationArea;

    private SwingWorker<Void, Void> joinWorker;

    public GameJoinFrame(Supplier<ClientSession> sessionSupplier) {
        super(sessionSupplier, "Battleship - Unirse a Partida");

        statusLabel = new JLabel("Ingresa el código de la partida.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusLabel.setName("statusLabel");

        gameCodeField = new JTextField();
        gameCodeField.setName("gameCodeField");

        JButton connectBtn = new JButton("Conectar");
        connectBtn.setName("connectButton");
        connectBtn.addActionListener(e -> connectToGame(connectBtn));

        JButton cancelBtn = new JButton("Cancelar / Volver");
        cancelBtn.setName("cancelButton");
        cancelBtn.addActionListener(e -> super.cancelAndReturn(joinWorker));

        infoArea = createReadOnlyArea("infoArea", 3);
        updateInfoDetails();

        notificationArea = registerNotificationArea(createReadOnlyArea("notificationArea", 8));

        JPanel formPanel = new JPanel(new BorderLayout(5, 5));
        formPanel.add(new JLabel("Código de partida:", SwingConstants.LEFT), BorderLayout.NORTH);
        formPanel.add(gameCodeField, BorderLayout.CENTER);
        formPanel.add(connectBtn, BorderLayout.EAST);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionsPanel.add(cancelBtn);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(createTitledScroll("Detalle de la conexión", infoArea));
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(createTitledScroll("Notificaciones del servidor", notificationArea));

        add(statusLabel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(actionsPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void updateInfoDetails() {
        StringBuilder sb = new StringBuilder();
        appendServerDetails(sb);
        infoArea.setText(sb.toString());
    }

    private void connectToGame(JButton connectBtn) {
        String input = gameCodeField.getText().trim();
        if (input.isEmpty()) {
            showWarning("Ingresa un código válido.", "Código requerido");
            return;
        }

        int gameId;
        try {
            gameId = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            showWarning("El código debe ser numérico.", "Formato inválido");
            return;
        }

        connectBtn.setEnabled(false);
        statusLabel.setText("Conectando a la partida " + gameId + "...");

        joinWorker = new SwingWorker<>() {
            private int jugadorId;
            private int partidaId;

            @Override
            protected Void doInBackground() throws Exception {
                if (isCancelled()) {
                    return null;
                }
                session.connect();
                if (isCancelled()) {
                    return null;
                }
                jugadorId = session.ensureJugador();
                if (isCancelled()) {
                    return null;
                }
                partidaId = session.unirsePartido(gameId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    handleJoinSuccess(jugadorId, partidaId);
                } catch (InterruptedException ex) {
                    handleJoinInterrupted(ex);
                } catch (ExecutionException ex) {
                    handleJoinFailure(ex);
                } catch (CancellationException ex) {
                    handleJoinCancelled();
                } finally {
                    connectBtn.setEnabled(true);
                }
            }
        };
        joinWorker.execute();
    }

    private void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void handleJoinSuccess(int jugadorId, int partidaId) {
        statusLabel.setText("Conectado a la partida. Preparando tablero...");
        updateInfoAfterJoin(jugadorId, partidaId);
        appendWelcomeMessages();
        openBoard();
    }

    private void handleJoinInterrupted(InterruptedException ex) {
        Thread.currentThread().interrupt();
        handleFailure(ex);
    }

    private void handleJoinFailure(ExecutionException ex) {
        Throwable cause = ex.getCause();
        handleFailure(cause instanceof Exception ? (Exception) cause : new Exception(cause));
    }

    private void handleJoinCancelled() {
        statusLabel.setText("Operación cancelada.");
    }

    private void handleFailure(Exception ex) {
        showFailureAndReturn(LOGGER,
                "No se pudo unir a la partida",
                "No se pudo unir a la partida: ",
                ex,
                joinWorker);
    }

    private void updateInfoAfterJoin(int jugadorId, int partidaId) {
        StringBuilder sb = new StringBuilder(infoArea.getText());
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }
        sb.append("Jugador ID: ").append(jugadorId).append('\n');
        sb.append("Partida ID: ").append(partidaId).append('\n');
        infoArea.setText(sb.toString());
    }

    void appendNotificationForTesting(String message) {
        appendNotification(message);
    }
}
