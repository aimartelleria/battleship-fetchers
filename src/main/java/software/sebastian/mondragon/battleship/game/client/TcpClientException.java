package software.sebastian.mondragon.battleship.game.client;

/**
 * Checked exception used to signal protocol or response errors reported by the TCP server.
 */
public class TcpClientException extends Exception {

    public TcpClientException(String message) {
        super(message);
    }

    public TcpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

