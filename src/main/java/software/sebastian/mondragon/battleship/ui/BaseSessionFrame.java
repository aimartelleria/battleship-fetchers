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

/**
 * Shared infrastructure for frames that interact with a {@link ClientSession}.
 * Handles notification wiring, cleanup and helper Swing components so concrete
 * frames can focus on their specific UI flow.
 */
abstract class BaseSessionFrame extends JFrame {
    protected final Supplier<ClientSession> sessionSupplier;
    protected final ClientSession session;
    private final java.util.function.Consumer<String> notificationConsumer;
    private JTextArea notificationArea;
    private boolean notificationsDetached;
    private boolean boardOpened;
    private boolean cleanedUp;

    protected BaseSessionFrame(Supplier<ClientSession> sessionSupplier, String title) {
        this.sessionSupplier = Objects.requireNonNull(sessionSupplier, "sessionSupplier");
        this.session = Objects.requireNonNull(sessionSupplier.get(), "session");
        setTitle(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        notificationConsumer = this::appendNotificationSafely;
        session.agregarSuscriptorNotificaciones(notificationConsumer);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanup();
            }
        });
    }

    protected final JTextArea createReadOnlyArea(String name, int rows) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setRows(rows);
        area.setName(name);
        return area;
    }

    protected final JScrollPane createTitledScroll(String title, JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    protected final JTextArea registerNotificationArea(JTextArea area) {
        this.notificationArea = Objects.requireNonNull(area, "notificationArea");
        return area;
    }

    protected final void appendWelcomeMessages() {
        List<String> welcome = session.getWelcomeMessages();
        for (String line : welcome) {
            appendNotification("[Servidor] " + line);
        }
    }

    protected final void appendNotification(String message) {
        appendNotificationSafely(message);
    }

    protected final void appendServerDetails(StringBuilder sb) {
        if (session.getHost() != null) {
            sb.append("Servidor: ").append(session.getHost());
            if (session.getPort() > 0) {
                sb.append(':').append(session.getPort());
            }
            sb.append('\n');
        }
    }

    private void appendNotificationSafely(String message) {
        JTextArea area = this.notificationArea;
        if (area == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            area.append(message + System.lineSeparator());
            area.setCaretPosition(area.getDocument().getLength());
            onNotificationAppended(message);
        });
    }

    protected void onNotificationAppended(String message) {
        // Subclasses may override to react to incoming messages.
    }

    protected final void openBoard() {
        if (boardOpened) {
            return;
        }
        boardOpened = true;
        detachNotifications();
        dispose();
        new GameBoardFrame(session, sessionSupplier);
    }

    protected final void cancelAndReturn(SwingWorker<?, ?> worker) {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        cleanup();
        dispose();
        new MainMenuFrame(sessionSupplier);
    }

    protected final void showFailureAndReturn(Logger logger, String logMessage, String dialogPrefix,
                                              Exception ex, SwingWorker<?, ?> worker) {
        logger.log(Level.SEVERE, logMessage, ex);
        JOptionPane.showMessageDialog(this,
                dialogPrefix + ex.getMessage(),
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
        cancelAndReturn(worker);
    }

    protected final void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        detachNotifications();
        if (!boardOpened) {
            try {
                session.close();
            } catch (IOException ignored) {
                // No action required during cleanup
            }
        }
    }

    private void detachNotifications() {
        if (notificationsDetached) {
            return;
        }
        notificationsDetached = true;
        session.quitarSuscriptorNotificaciones(notificationConsumer);
    }
}
