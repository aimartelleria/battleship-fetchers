package software.sebastian.mondragon.battleship.ui;

import software.sebastian.mondragon.battleship.game.client.ClientSession;

import javax.swing.*;
import java.awt.*;
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
            JOptionPane.showMessageDialog(this, "Ingresa un código válido.", "Código requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int gameId;
        try {
            gameId = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El código debe ser numérico.", "Formato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        connectBtn.setEnabled(false);
        statusLabel.setText("Conectando a la partida " + gameId + "...");

        joinWorker = new SwingWorker<>() {
            private int jugadorId;
            private int partidaId;

            @Override
            protected Void doInBackground() throws Exception {
                session.connect();
                jugadorId = session.ensureJugador();
                partidaId = session.unirsePartido(gameId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    updateInfoAfterJoin(jugadorId, partidaId);
                    statusLabel.setText("Conectado a la partida. Preparando tablero...");
                    appendWelcomeMessages();
                    openBoard();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    showFailureAndReturn(LOGGER,
                            "No se pudo unir a la partida",
                            "No se pudo unir a la partida: ",
                            interrupted,
                            joinWorker);
                } catch (ExecutionException ex) {
                    showFailureAndReturn(LOGGER,
                            "No se pudo unir a la partida",
                            "No se pudo unir a la partida: ",
                            ex,
                            joinWorker);
                } finally {
                    connectBtn.setEnabled(true);
                }
            }
        };
        joinWorker.execute();
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
