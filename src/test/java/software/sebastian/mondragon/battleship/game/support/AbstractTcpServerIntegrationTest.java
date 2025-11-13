package software.sebastian.mondragon.battleship.game.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.sebastian.mondragon.battleship.game.server.TcpServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Utility base class that starts and stops a {@link TcpServer} on a random port
 * for integration-style tests. Subclasses can override {@link #createServer(int)}
 * to customize the server instance if needed.
 */
public abstract class AbstractTcpServerIntegrationTest {
    protected TcpServer server;
    protected int port;

    @BeforeEach
    void startTcpServer() throws Exception {
        port = findFreePort();
        server = createServer(port);
        server.start();
    }

    @AfterEach
    void stopTcpServer() {
        if (server != null) {
            server.stop();
        }
    }

    protected TcpServer createServer(int port) {
        return new TcpServer(port);
    }

    private int findFreePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }
}
