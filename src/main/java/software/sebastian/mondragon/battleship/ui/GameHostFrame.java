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
//  GAME HOST FRAME
// ----------------------------------
public class GameHostFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(GameHostFrame.class.getName());

    private final transient ClientSession session;
    private final transient Supplier<ClientSession> sessionSupplier;
    private final JLabel statusLabel;
    private final JTextArea infoArea;
    private final JTextArea notificationArea;
    private final transient java.util.function.Consumer<String> notificationConsumer;

    private volatile boolean boardOpened;
    private boolean cleanedUp;
    private transient SwingWorker<Void, Void> worker;

    public GameHostFrame(Supplier<ClientSession> sessionSupplier) {
        this.sessionSupplier = Objects.requireNonNull(sessionSupplier, "sessionSupplier");
        this.session = Objects.requireNonNull(sessionSupplier.get(), "session");

        setTitle("Battleship - Crear Partida");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel("Conectando con el servidor...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusLabel.setName("statusLabel");

        infoArea = createTextArea();
        infoArea.setRows(4);
        infoArea.setName("infoArea");
        notificationArea = createTextArea();
        notificationArea.setRows(8);
        notificationArea.setName("notificationArea");

        JButton cancelBtn = new JButton("Cancelar / Volver");
        cancelBtn.setName("cancelButton");
        cancelBtn.addActionListener(e -> cancelAndReturn());

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(createTitledScroll("Detalle de la partida", infoArea));
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(createTitledScroll("Notificaciones del servidor", notificationArea));

        add(statusLabel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(cancelBtn, BorderLayout.SOUTH);

        notificationConsumer = this::appendNotificationSafely;
        session.agregarSuscriptorNotificaciones(notificationConsumer);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanup();
            }
        });

        startHosting();
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

    private void startHosting() {
        worker = new SwingWorker<>() {
            private int jugadorId;
            private int partidoId;

            @Override
            protected Void doInBackground() throws Exception {
                session.connect();
                jugadorId = session.ensureJugador();
                partidoId = session.crearPartido();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Esperando al segundo jugador...");
                    updateInfoArea(jugadorId, partidoId);
                    appendWelcomeMessages();
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    Throwable cause = ex.getCause();
                    if (cause instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    handleFailure(ex);
                }
            }
        };
        worker.execute();
    }

    private void appendWelcomeMessages() {
        List<String> welcome = session.getWelcomeMessages();
        for (String line : welcome) {
            appendNotificationSafely("[Servidor] " + line);
        }
    }

    private void updateInfoArea(int jugadorId, int partidoId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Jugador ID: ").append(jugadorId).append('\n');
        sb.append("Código de partida: ").append(partidoId).append('\n');
        if (session.getHost() != null) {
            sb.append("Servidor: ").append(session.getHost());
            if (session.getPort() > 0) {
                sb.append(':').append(session.getPort());
            }
            sb.append('\n');
        }
        infoArea.setText(sb.toString());
    }

    private void appendNotificationSafely(String message) {
        SwingUtilities.invokeLater(() -> {
            notificationArea.append(message + System.lineSeparator());
            notificationArea.setCaretPosition(notificationArea.getDocument().getLength());
            Integer partidoId = session.getPartidoId();
            if (!boardOpened && partidoId != null && message.contains("Partida " + partidoId + " iniciada")) {
                openBoard();
            }
        });
    }

    private void openBoard() {
        boardOpened = true;
        session.quitarSuscriptorNotificaciones(notificationConsumer);
        dispose();
        new GameBoardFrame(session, sessionSupplier);
    }

    private void handleFailure(Exception ex) {
        LOGGER.log(Level.SEVERE, "No se pudo crear la partida", ex);
        JOptionPane.showMessageDialog(this,
                "No se pudo crear la partida: " + ex.getMessage(),
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
        cancelAndReturn();
    }

    private void cancelAndReturn() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
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
