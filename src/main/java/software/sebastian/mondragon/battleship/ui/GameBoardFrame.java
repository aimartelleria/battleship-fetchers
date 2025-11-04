package software.sebastian.mondragon.battleship.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameBoardFrame extends JFrame {
    private JButton[][] ownGrid = new JButton[10][10];
    private JButton[][] enemyGrid = new JButton[10][10];
    private JLabel statusLabel;
    private JPanel selectedShip; // barco actualmente arrastrado
    private int selectedShipSize;
    private boolean horizontal = true; // dirección del barco
    private JButton rotateBtn;

    public GameBoardFrame() {
        setTitle("Battleship - Tablero de Juego");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        JLabel title = new JLabel("BATTLESHIP", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        // Panel central (barcos + tableros)
        JPanel boardsPanel = new JPanel(new BorderLayout(10, 10));
        JPanel shipsPanel = createShipsPanel();
        JPanel gridsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        gridsPanel.add(createBoardPanel("Tu tablero (colocar barcos)", ownGrid, true));
        gridsPanel.add(createBoardPanel("Tablero enemigo", enemyGrid, false));

        boardsPanel.add(shipsPanel, BorderLayout.WEST);
        boardsPanel.add(gridsPanel, BorderLayout.CENTER);

        // Panel inferior
        JButton readyBtn = new JButton("Listo ✔");
        rotateBtn = new JButton("Dirección: Horizontal");
        statusLabel = new JLabel("Estado: Esperando al otro jugador...");

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.add(readyBtn);
        bottomPanel.add(rotateBtn);
        bottomPanel.add(statusLabel);

        // Eventos
        readyBtn.addActionListener(e -> {
            readyBtn.setEnabled(false);
            statusLabel.setText("Listo! Esperando al otro jugador...");
        });

        rotateBtn.addActionListener(e -> toggleDirection());

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(boardsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    // ============================
    // TABLEROS
    // ============================
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
                                placeShip(row, col);
                            }
                        }
                    });
                } else {
                    cell.addActionListener(e -> cell.setBackground(Color.BLUE));
                }

                gridPanel.add(cell);
            }
        }

        panel.add(label, BorderLayout.NORTH);
        panel.add(gridPanel, BorderLayout.CENTER);
        return panel;
    }

    // ============================
    // PANEL DE BARCOS DISPONIBLES
    // ============================
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

    // Crea un barco arrastrable
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
                if (ship != null)
                    ship.setBorder(null);
            }
        });

        return ship;
    }

    // ============================
    // CAMBIO DE DIRECCIÓN
    // ============================
    private void toggleDirection() {
        horizontal = !horizontal;
        rotateBtn.setText("Dirección: " + (horizontal ? "Horizontal" : "Vertical"));
    }

    // ============================
    // COLOCAR BARCO EN EL TABLERO
    // ============================
    private void placeShip(int startRow, int startCol) {
        if (selectedShip == null) return;

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

        paintShip(startRow, startCol, rowStep, colStep);
        deactivateSelectedShip();
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
        selectedShip.setEnabled(false);
        selectedShip.setOpaque(false);
        selectedShip = null;
        selectedShipSize = 0;
    }
}
