package software.sebastian.mondragon.battleship.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// ----------------------------------
//  GAME HOST FRAME
// ----------------------------------
public class GameHostFrame extends JFrame {

    public GameHostFrame() {
        setTitle("Battleship - Crear Partida");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(350, 200);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel waitingLabel = new JLabel("Esperando jugador...", SwingConstants.CENTER);
        waitingLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        JTextArea infoArea = new JTextArea("Código de partida: 12345\nIP Local: 192.168.1.10");
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        infoArea.setBackground(getBackground());

        JButton cancelBtn = new JButton("Cancelar / Volver");

        add(waitingLabel, BorderLayout.NORTH);
        add(infoArea, BorderLayout.CENTER);
        add(cancelBtn, BorderLayout.SOUTH);

        // Eventos
        cancelBtn.addActionListener(e -> {
            dispose();
            new MainMenuFrame();
        });

        // Simular detección de jugador conectado
        Timer timer = new Timer(3000, e -> {
            JOptionPane.showMessageDialog(this, "Jugador conectado!");
            dispose();
            new GameBoardFrame(true);
        });
        timer.setRepeats(false);
        timer.start();

        setVisible(true);
    }
}