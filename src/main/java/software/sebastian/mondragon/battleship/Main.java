package software.sebastian.mondragon.battleship;

import software.sebastian.mondragon.battleship.ui.MainMenuFrame;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.SwingUtilities.invokeLater(MainMenuFrame::new);
        });
    }
}