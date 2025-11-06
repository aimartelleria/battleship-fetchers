package software.sebastian.mondragon.battleship.ui;

import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.game.client.TcpClient;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class GameBoardFrame extends JFrame {
    private final transient ClientSession session;
    private final transient Supplier<ClientSession> sessionSupplier;
    private final JButton[][] ownGrid = new JButton[10][10];
    private final JButton[][] enemyGrid = new JButton[10][10];
    private final JLabel statusLabel;
    private JPanel selectedShip;
    private int selectedShipSize;
    private boolean horizontal = true;
    private JButton rotateBtn;
    private boolean cleanedUp;
    private final transient java.util.function.Consumer<String> notificationConsumer;

    public GameBoardFrame(ClientSession session, Supplier<ClientSession> sessionSupplier) {
        this.session = Objects.requireNonNull(session, "session");
        this.sessionSupplier = Objects.requireNonNull(sessionSupplier, "sessionSupplier");

        setTitle("Battleship - Tablero de Juego");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        JLabel title = new JLabel("BATTLESHIP - Jugador " + session.getJugadorId(), SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel boardsPanel = new JPanel(new BorderLayout(10, 10));
        JPanel shipsPanel = createShipsPanel();
        JPanel gridsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        gridsPanel.add(createBoardPanel("Tu tablero (colocar barcos)", ownGrid, true));
        gridsPanel.add(createBoardPanel("Tablero enemigo", enemyGrid, false));
        boardsPanel.add(shipsPanel, BorderLayout.WEST);
        boardsPanel.add(gridsPanel, BorderLayout.CENTER);

        JButton readyBtn = new JButton("Listo ✔");
        readyBtn.setName("readyButton");
        rotateBtn = new JButton("Dirección: Horizontal");
        rotateBtn.setName("rotateButton");
        JButton exitBtn = new JButton("Salir al menú");
        exitBtn.setName("exitButton");
        statusLabel = new JLabel("Estado: Conectado. Coloca tus barcos.");
        statusLabel.setName("statusLabel");

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottomPanel.add(exitBtn);
        bottomPanel.add(readyBtn);
        bottomPanel.add(rotateBtn);
        bottomPanel.add(statusLabel);

        readyBtn.addActionListener(e -> markReady(readyBtn));
        rotateBtn.addActionListener(e -> toggleDirection());
        exitBtn.addActionListener(e -> exitToMenu());

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(boardsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        notificationConsumer = this::handleNotification;
        session.agregarSuscriptorNotificaciones(notificationConsumer);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanup();
            }
        });

        setVisible(true);
    }

    private JPanel createBoardPanel(String title, JButton[][] grid, boolean own) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        JPanel gridPanel = new JPanel(new GridLayout(10, 10));

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                JButton cell = new JButton();
                cell.setPreferredSize(new Dimension(40, 40));
                cell.setBackground(Color.WHITE);
                grid[i][j] = cell;

                if (own) {
                    int row = i;
                    int col = j;
                    cell.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseReleased(MouseEvent e) {
                            if (selectedShip != null && selectedShip.isEnabled()) {
                                attemptPlaceShip(row, col);
                            }
                        }
                    });
                } else {
                    int row = i;
                    int col = j;
                    cell.addActionListener(e -> shootAt(row, col, cell));
                }

                gridPanel.add(cell);
            }
        }

        panel.add(label, BorderLayout.NORTH);
        panel.add(gridPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createShipsPanel() {
        JPanel shipsPanel = new JPanel();
        shipsPanel.setLayout(new BoxLayout(shipsPanel, BoxLayout.Y_AXIS));
        shipsPanel.setBorder(BorderFactory.createTitledBorder("Barcos disponibles"));
        shipsPanel.setPreferredSize(new Dimension(200, 0));

        shipsPanel.add(createDraggableShip(2, Color.DARK_GRAY));
        shipsPanel.add(Box.createVerticalStrut(10));
        shipsPanel.add(createDraggableShip(3, Color.GRAY));
        shipsPanel.add(Box.createVerticalStrut(10));
        shipsPanel.add(createDraggableShip(3, Color.GRAY));
        shipsPanel.add(Box.createVerticalStrut(10));
        shipsPanel.add(createDraggableShip(4, Color.LIGHT_GRAY));
        shipsPanel.add(Box.createVerticalStrut(10));
        shipsPanel.add(createDraggableShip(5, Color.LIGHT_GRAY));

        return shipsPanel;
    }

    private JPanel createDraggableShip(int size, Color color) {
        JPanel ship = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        ship.setName("ship-" + size);
        ship.setCursor(new Cursor(Cursor.HAND_CURSOR));

        for (int i = 0; i < size; i++) {
            JPanel cell = new JPanel();
            cell.setBackground(color);
            cell.setPreferredSize(new Dimension(25, 25));
            cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            ship.add(cell);
        }

        JLabel label = new JLabel(" (" + size + ")");
        ship.add(label);

        ship.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!ship.isEnabled()) return;
                selectedShip = ship;
                selectedShipSize = size;
                ship.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ship.setBorder(null);
            }
        });

        return ship;
    }

    private void markReady(JButton readyBtn) {
        readyBtn.setEnabled(false);
        statusLabel.setText("Estado: Listo. Esperando al oponente...");
    }

    private void toggleDirection() {
        horizontal = !horizontal;
        rotateBtn.setText("Dirección: " + (horizontal ? "Horizontal" : "Vertical"));
    }

    private void attemptPlaceShip(int startRow, int startCol) {
        if (selectedShip == null) {
            return;
        }

        int rowStep = horizontal ? 0 : 1;
        int colStep = horizontal ? 1 : 0;

        if (!isPlacementWithinBounds(startRow, startCol, rowStep, colStep)) {
            String orientation = horizontal ? "horizontalmente" : "verticalmente";
            JOptionPane.showMessageDialog(this, "El barco no cabe " + orientation + " aquí.");
            return;
        }

        if (hasOverlap(startRow, startCol, rowStep, colStep)) {
            JOptionPane.showMessageDialog(this, "Ya hay un barco en esa posición.");
            return;
        }

        List<int[]> coordinates = buildCoordinates(startRow, startCol, rowStep, colStep);
        try {
            TcpClient.ShipPlacementResult result = session.colocarBarco(coordinates);
            paintShip(startRow, startCol, rowStep, colStep);
            deactivateSelectedShip();
            statusLabel.setText("Estado: Barco " + result.shipId() + " colocado (" + result.size() + " casillas)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo colocar el barco: " + ex.getMessage(),
                    "Error de servidor", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<int[]> buildCoordinates(int startRow, int startCol, int rowStep, int colStep) {
        List<int[]> coords = new ArrayList<>();
        int row = startRow;
        int col = startCol;
        for (int i = 0; i < selectedShipSize; i++) {
            coords.add(new int[]{row, col});
            row += rowStep;
            col += colStep;
        }
        return coords;
    }

    private boolean isPlacementWithinBounds(int startRow, int startCol, int rowStep, int colStep) {
        int endRow = startRow + rowStep * (selectedShipSize - 1);
        int endCol = startCol + colStep * (selectedShipSize - 1);
        return isInsideGrid(startRow, startCol) && isInsideGrid(endRow, endCol);
    }

    private boolean isInsideGrid(int row, int col) {
        return row >= 0 && row < ownGrid.length && col >= 0 && col < ownGrid[0].length;
    }

    private boolean hasOverlap(int startRow, int startCol, int rowStep, int colStep) {
        int row = startRow;
        int col = startCol;
        for (int i = 0; i < selectedShipSize; i++) {
            if (!ownGrid[row][col].isEnabled()) {
                return true;
            }
            row += rowStep;
            col += colStep;
        }
        return false;
    }

    private void paintShip(int startRow, int startCol, int rowStep, int colStep) {
        int row = startRow;
        int col = startCol;
        for (int i = 0; i < selectedShipSize; i++) {
            ownGrid[row][col].setBackground(Color.GRAY);
            ownGrid[row][col].setEnabled(false);
            row += rowStep;
            col += colStep;
        }
    }

    private void deactivateSelectedShip() {
        if (selectedShip != null) {
            selectedShip.setEnabled(false);
            selectedShip.setOpaque(false);
            selectedShip.setBorder(null);
        }
        selectedShip = null;
        selectedShipSize = 0;
    }

    private void shootAt(int row, int col, JButton cell) {
        if (!cell.isEnabled()) {
            return;
        }
        statusLabel.setText("Estado: Disparando a (" + row + ", " + col + ")...");
        try {
            ResultadoDisparo resultado = session.disparar(row, col);
            applyShotResult(cell, resultado);
            statusLabel.setText("Estado: Disparo a (" + row + ", " + col + "): " + resultado);
            cell.setEnabled(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo realizar el disparo: " + ex.getMessage(),
                    "Error de juego", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyShotResult(JButton cell, ResultadoDisparo resultado) {
        switch (resultado) {
            case AGUA -> cell.setBackground(Color.BLUE);
            case TOCADO -> cell.setBackground(Color.ORANGE);
            case HUNDIDO -> cell.setBackground(Color.RED);
            default -> cell.setBackground(Color.CYAN);
        }
    }

    private void handleNotification(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Estado: " + message));
    }

    private void exitToMenu() {
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
        try {
            session.close();
        } catch (IOException e) {
            // Ignored
        }
    }
}
