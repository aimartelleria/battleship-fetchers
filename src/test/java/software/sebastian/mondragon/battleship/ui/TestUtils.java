package software.sebastian.mondragon.battleship.ui;

import javax.swing.*;
import java.awt.*;

public class TestUtils {
    public static Component findComponentByText(Container container, String text) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().equals(text)) return c;
            if (c instanceof Container) {
                Component result = findComponentByText((Container) c, text);
                if (result != null) return result;
            }
        }
        return null;
    }

    public static Component findComponentByName(Container container, String name) {
        for (Component c : container.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container) {
                Component result = findComponentByName((Container) c, name);
                if (result != null) return result;
            }
        }
        return null;
    }

    public static JButton findButtonByPosition(Container container, int row, int col) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton && ((JButton) c).isVisible()) {
                return (JButton) c;
            }
            if (c instanceof Container) {
                JButton result = findButtonByPosition((Container) c, row, col);
                if (result != null) return result;
            }
        }
        return null;
    }
}
