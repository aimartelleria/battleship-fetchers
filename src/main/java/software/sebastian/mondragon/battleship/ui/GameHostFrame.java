package software.sebastian.mondragon.battleship.ui;

import software.sebastian.mondragon.battleship.game.client.ClientSession;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Logger;

// ----------------------------------
//  GAME HOST FRAME
// ----------------------------------
@SuppressWarnings("serial")
public class GameHostFrame extends BaseSessionFrame {
    private static final Logger LOGGER = Logger.getLogger(GameHostFrame.class.getName());

    private final JLabel statusLabel;
    private final JTextArea infoArea;
    private final JTextArea notificationArea;
    private SwingWorker<Void, Void> worker;

    public GameHostFrame(Supplier<ClientSession> sessionSupplier) {
        super(sessionSupplier, "Battleship - Crear Partida");

        statusLabel = new JLabel("Conectando con el servidor...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusLabel.setName("statusLabel");

        infoArea = createReadOnlyArea("infoArea", 4);
        notificationArea = registerNotificationArea(createReadOnlyArea("notificationArea", 8));

        JButton cancelBtn = new JButton("Cancelar / Volver");
        cancelBtn.setName("cancelButton");
        cancelBtn.addActionListener(e -> super.cancelAndReturn(worker));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(createTitledScroll("Detalle de la partida", infoArea));
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(createTitledScroll("Notificaciones del servidor", notificationArea));

        add(statusLabel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(cancelBtn, BorderLayout.SOUTH);

        startHosting();
        setVisible(true);
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
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    showFailureAndReturn(LOGGER,
                            "No se pudo crear la partida",
                            "No se pudo crear la partida: ",
                            interrupted,
                            worker);
                } catch (ExecutionException ex) {
                    showFailureAndReturn(LOGGER,
                            "No se pudo crear la partida",
                            "No se pudo crear la partida: ",
                            ex,
                            worker);
                }
            }
        };
        worker.execute();
    }

    private void updateInfoArea(int jugadorId, int partidoId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Jugador ID: ").append(jugadorId).append('\n');
        sb.append("C\u00f3digo de partida: ").append(partidoId).append('\n');
        appendServerDetails(sb);
        infoArea.setText(sb.toString());
    }

    @Override
    protected void onNotificationAppended(String message) {
        Integer partidoId = session.getPartidoId();
        if (partidoId != null && message.contains("Partida " + partidoId + " iniciada")) {
            openBoard();
        }
    }
}
