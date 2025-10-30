package software.sebastian.mondragon.battleship.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// ----------------------------------
//  GAME JOIN FRAME
// ----------------------------------
public class GameJoinFrame extends JFrame {

    public GameJoinFrame() {
        setTitle("Battleship - Unirse a Partida");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(350, 200);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel label = new JLabel("Código de partida o IP del servidor:", SwingConstants.CENTER);
        JTextField codeField = new JTextField();
        JButton connectBtn = new JButton("Conectar");

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(label, BorderLayout.NORTH);
        centerPanel.add(codeField, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
        add(connectBtn, BorderLayout.SOUTH);

        // Evento conectar
        connectBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Conectado exitosamente a " + codeField.getText());
            dispose();
            new GameBoardFrame(false);
        });

        setVisible(true);
    }
}
