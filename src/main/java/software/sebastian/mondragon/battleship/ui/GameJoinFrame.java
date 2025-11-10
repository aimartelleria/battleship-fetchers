package software.sebastian.mondragon.battleship.ui;

import software.sebastian.mondragon.battleship.game.client.ClientSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

// ----------------------------------
//  GAME JOIN FRAME
// ----------------------------------
public class GameJoinFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(GameJoinFrame.class.getName());

    private final Supplier<ClientSession> sessionSupplier;
    private final ClientSession session;
    private final JTextField gameCodeField;
    private final JLabel statusLabel;
    private final JTextArea infoArea;
    private final JTextArea notificationArea;
    private final java.util.function.Consumer<String> notificationConsumer;

    private boolean cleanedUp;
    private boolean boardOpened;
    private SwingWorker<Void, Void> joinWorker;

    public GameJoinFrame(Supplier<ClientSession> sessionSupplier) {
        this.sessionSupplier = Objects.requireNonNull(sessionSupplier, "sessionSupplier");
        this.session = Objects.requireNonNull(sessionSupplier.get(), "session");

        setTitle("Battleship - Unirse a Partida");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

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
        cancelBtn.addActionListener(e -> cancelAndReturn());

        infoArea = createTextArea();
        infoArea.setRows(3);
        infoArea.setName("infoArea");
        updateInfoDetails();

        notificationArea = createTextArea();
        notificationArea.setRows(8);
        notificationArea.setName("notificationArea");

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

        notificationConsumer = this::appendNotificationSafely;
        session.agregarSuscriptorNotificaciones(notificationConsumer);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanup();
            }
        });

        setVisible(true);
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return area;
    }

    private JScrollPane createTitledScroll(String title, JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private void updateInfoDetails() {
        StringBuilder sb = new StringBuilder();
        if (session.getHost() != null) {
            sb.append("Servidor: ").append(session.getHost());
            if (session.getPort() > 0) {
                sb.append(':').append(session.getPort());
            }
            sb.append('\n');
        }
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
                } catch (Exception ex) {
                    handleFailure(ex);
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

    private void appendWelcomeMessages() {
        List<String> welcome = session.getWelcomeMessages();
        for (String line : welcome) {
            appendNotificationSafely("[Servidor] " + line);
        }
    }

    private void appendNotificationSafely(String message) {
        SwingUtilities.invokeLater(() -> {
            notificationArea.append(message + System.lineSeparator());
            notificationArea.setCaretPosition(notificationArea.getDocument().getLength());
        });
    }

    private void openBoard() {
        if (boardOpened) {
            return;
        }
        boardOpened = true;
        session.quitarSuscriptorNotificaciones(notificationConsumer);
        dispose();
        new GameBoardFrame(session, sessionSupplier);
    }

    private void handleFailure(Exception ex) {
        LOGGER.log(Level.SEVERE, "No se pudo unir a la partida", ex);
        JOptionPane.showMessageDialog(this,
                "No se pudo unir a la partida: " + ex.getMessage(),
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
        cancelAndReturn();
    }

    private void cancelAndReturn() {
        if (joinWorker != null && !joinWorker.isDone()) {
            joinWorker.cancel(true);
        }
        cleanup();
        dispose();
        new MainMenuFrame(sessionSupplier);
    }

    private void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        session.quitarSuscriptorNotificaciones(notificationConsumer);
        if (!boardOpened) {
            try {
                session.close();
            } catch (IOException e) {
                // Ignored
            }
        }
    }
}
