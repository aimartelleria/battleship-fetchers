package software.sebastian.mondragon.battleship.ui.support;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Assumptions;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Helper utilities to bootstrap and tear down Swing frames inside tests
 * while gracefully skipping execution on headless environments.
 */
public final class SwingTestSupport {
    private SwingTestSupport() {
    }

    public static <T extends JFrame> FrameFixture showFrame(Supplier<T> frameSupplier) {
        assumeGraphicsAvailable();
        T frame = GuiActionRunner.execute(frameSupplier::get);
        FrameFixture window = new FrameFixture(frame);
        window.show();
        return window;
    }

    public static void cleanup(FrameFixture window) {
        if (window != null) {
            window.cleanUp();
        }
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible()) {
                frame.dispose();
            }
        }
    }

    private static void assumeGraphicsAvailable() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Entorno headless: se omiten pruebas de interfaz Swing");
        System.setProperty("java.awt.headless", "false");
    }
}
