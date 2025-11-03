# Battleship Game TODO List

## TCP Server

- [ ] Create a `Server` class with a `main` method to start the server.
- [ ] Implement a `ServerSocket` to listen for client connections on a specific port.
- [ ] For each client connection, spawn a new `Thread` to handle communication with that client.
- [ ] Create a `ClientHandler` class (implementing `Runnable`) to manage communication with a single client.
- [ ] Implement a mechanism to manage game sessions and associate clients with games.

## Communication Protocol

- [ ] Define a clear text-based or JSON-based protocol for client-server communication.
- [ ] Protocol should include messages for:
    - [ ] Creating a new game.
    - [ ] Joining an existing game.
    - [ ] Placing ships.
    - [ ] Shooting at a coordinate.
    - [ ] Receiving game state updates (e.g., shot results, turn changes, game over).
    - [ ] Handling errors and notifications.

## Swing Client

- [ ] Create a `GameClient` class with a `main` method to launch the Swing GUI.
- [ ] Design and implement the main game window using `JFrame`.
- [ ] Create a `BoardPanel` class (extending `JPanel`) to display the game boards (player's and opponent's).
- [ ] Implement mouse listeners on the opponent's board to handle shooting.
- [ ] Add components to display game status, notifications, and whose turn it is.
- [ ] Implement the client-side networking to connect to the server and send/receive protocol messages.
- [ ] Update the GUI based on messages received from the server.

## Integration

- [ ] Modify the `GameService` to be used by the server-side logic.
- [ ] The `ClientHandler` will interact with the `GameService` based on client messages.
- [ ] The `Notifier` in `GameService` should be adapted to send messages to clients through their respective `ClientHandler` threads.
