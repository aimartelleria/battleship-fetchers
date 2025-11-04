package software.sebastian.mondragon.battleship.ui;

import javax.swing.*;
import java.awt.*;

// ----------------------------------
//  MAIN MENU FRAME (versión testeable)
// ----------------------------------
public class MainMenuFrame extends JFrame {

    public MainMenuFrame() {
        setTitle("Battleship - Menú Principal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(300, 250);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Battleship", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        JButton crearBtn = new JButton("Crear partida");
        JButton unirseBtn = new JButton("Unirse a partida");
        JButton salirBtn = new JButton("Salir");

        // 🔹 Añadimos nombres internos para AssertJ Swing
        crearBtn.setName("crearBtn");
        unirseBtn.setName("unirseBtn");
        salirBtn.setName("salirBtn");

        buttonPanel.add(crearBtn);
        buttonPanel.add(unirseBtn);
        buttonPanel.add(salirBtn);
        add(buttonPanel, BorderLayout.CENTER);

        // Eventos
        crearBtn.addActionListener(e -> {
            dispose();
            new GameHostFrame();
        });

        unirseBtn.addActionListener(e -> {
            dispose();
            new GameJoinFrame();
        });

        salirBtn.addActionListener(e -> onExit());

        setVisible(true);
    }

    protected void onExit() {
        System.exit(0);
    }
}
