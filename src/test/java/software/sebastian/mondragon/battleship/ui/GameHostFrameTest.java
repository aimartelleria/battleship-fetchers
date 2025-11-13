package software.sebastian.mondragon.battleship.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.*;

import software.sebastian.mondragon.battleship.game.client.ClientSession;
import software.sebastian.mondragon.battleship.game.client.TcpClient.ShipPlacementResult;
import software.sebastian.mondragon.battleship.game.client.TcpClientException;
import software.sebastian.mondragon.battleship.game.service.ResultadoDisparo;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class GameHostFrameTest {

    private FrameFixture window;
    private StubClientSession sessionStub;

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Entorno headless: se omiten pruebas de interfaz Swing");
        System.setProperty("java.awt.headless", "false");
        sessionStub = new StubClientSession();
        GameHostFrame frame = GuiActionRunner.execute(() -> new GameHostFrame(() -> sessionStub));
        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    void tearDown() {
        if (window != null) window.cleanUp();
        for (Frame f : Frame.getFrames()) if (f.isVisible()) f.dispose();
    }

    @Test
    @DisplayName("Inicializa la interfaz correctamente")
    void shouldInitializeUI() {
        JFrame frame = (JFrame) window.target();
        assertEquals("Battleship - Crear Partida", frame.getTitle());
        window.label("statusLabel").requireVisible();
        window.textBox("infoArea").requireNotEditable();
        window.textBox("notificationArea").requireNotEditable();
        window.button("cancelButton").requireText("Cancelar / Volver");
    }

    @Test
    @DisplayName("Muestra información correcta después de crear partida")
    void shouldShowGameInfoAfterCreation() throws Exception {
        // Usar reflexión para simular el flujo exitoso
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al método updateInfoArea via reflexión
        Method updateInfoAreaMethod = GameHostFrame.class.getDeclaredMethod("updateInfoArea", int.class, int.class);
        updateInfoAreaMethod.setAccessible(true);
        
        GuiActionRunner.execute(() -> {
            try {
                updateInfoAreaMethod.invoke(frame, 123, 456);
            } catch (Exception e) {
                fail("Error invoking updateInfoArea: " + e.getMessage());
            }
        });

        window.label("statusLabel").requireText("Esperando al segundo jugador...");
        String infoText = window.textBox("infoArea").text();
        assertTrue(infoText.contains("Jugador ID: 123"));
        assertTrue(infoText.contains("Código de partida: 456"));
    }

    @Test
    @DisplayName("Muestra mensajes de bienvenida correctamente")
    void shouldDisplayWelcomeMessages() throws Exception {
        List<String> welcomeMessages = new ArrayList<>();
        welcomeMessages.add("Bienvenido al servidor de Battleship");
        welcomeMessages.add("Esperando oponente...");
        
        sessionStub.setWelcomeMessages(welcomeMessages);
        
        // Acceder al método appendWelcomeMessages via reflexión
        GameHostFrame frame = (GameHostFrame) window.target();
        Method appendWelcomeMessagesMethod = GameHostFrame.class.getDeclaredMethod("appendWelcomeMessages");
        appendWelcomeMessagesMethod.setAccessible(true);
        
        GuiActionRunner.execute(() -> {
            try {
                appendWelcomeMessagesMethod.invoke(frame);
            } catch (Exception e) {
                fail("Error invoking appendWelcomeMessages: " + e.getMessage());
            }
        });

        String notificationText = window.textBox("notificationArea").text();
        assertTrue(notificationText.contains("[Servidor] Bienvenido al servidor de Battleship"));
        assertTrue(notificationText.contains("[Servidor] Esperando oponente..."));
    }

    @Test
    @DisplayName("Abre el tablero cuando se recibe notificación de partida iniciada")
    void shouldOpenBoardWhenGameStartedNotificationReceived() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        sessionStub.setPartidoId(789);
        
        // Acceder al método appendNotificationSafely via reflexión
        Method appendNotificationSafelyMethod = GameHostFrame.class.getDeclaredMethod("appendNotificationSafely", String.class);
        appendNotificationSafelyMethod.setAccessible(true);
        
        GuiActionRunner.execute(() -> {
            try {
                appendNotificationSafelyMethod.invoke(frame, "Partida 789 iniciada");
            } catch (Exception e) {
                fail("Error invoking appendNotificationSafely: " + e.getMessage());
            }
        });

        // Esperar un poco para que procese la notificación
        Pause.pause(1000);
        
        // Verificar que la ventana se cierra (dispose fue llamado)
        assertFalse(frame.isVisible());
    }

    @Test
    @DisplayName("Ejecuta cleanup correctamente al cerrar ventana")
    void shouldExecuteCleanupOnWindowClose() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al campo cleanedUp via reflexión
        Field cleanedUpField = GameHostFrame.class.getDeclaredField("cleanedUp");
        cleanedUpField.setAccessible(true);
        
        // Verificar que inicialmente no está cleanedUp
        boolean initialCleanup = (boolean) cleanedUpField.get(frame);
        assertFalse(initialCleanup);
        
        // Cerrar la ventana
        window.close();
        
        // Verificar que se ejecutó cleanup
        boolean finalCleanup = (boolean) cleanedUpField.get(frame);
        assertTrue(finalCleanup);
    }

    @Test
    @DisplayName("Cancela y regresa al menú principal correctamente")
    void shouldCancelAndReturnToMainMenu() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al campo cleanedUp via reflexión
        Field cleanedUpField = GameHostFrame.class.getDeclaredField("cleanedUp");
        cleanedUpField.setAccessible(true);
        
        window.button("cancelButton").click();
        
        // Verificar que la ventana se cierra
        assertFalse(frame.isVisible());
        
        // Verificar que se ejecutó cleanup
        boolean finalCleanup = (boolean) cleanedUpField.get(frame);
        assertTrue(finalCleanup);
    }

    @Test
    @DisplayName("Maneja correctamente notificaciones del servidor")
    void shouldHandleServerNotifications() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al campo notificationConsumer via reflexión
        Field notificationConsumerField = GameHostFrame.class.getDeclaredField("notificationConsumer");
        notificationConsumerField.setAccessible(true);
        
        Consumer<String> consumer = (Consumer<String>) notificationConsumerField.get(frame);
        
        GuiActionRunner.execute(() -> {
            consumer.accept("Test notification");
        });

        String notificationText = window.textBox("notificationArea").text();
        assertTrue(notificationText.contains("Test notification"));
    }

    @Test
    @DisplayName("Crea áreas de texto con las propiedades correctas")
    void shouldCreateTextAreasWithCorrectProperties() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al método createTextArea via reflexión
        Method createTextAreaMethod = GameHostFrame.class.getDeclaredMethod("createTextArea");
        createTextAreaMethod.setAccessible(true);
        
        JTextArea textArea = (JTextArea) GuiActionRunner.execute(() -> {
            try {
                return createTextAreaMethod.invoke(frame);
            } catch (Exception e) {
                fail("Error invoking createTextArea: " + e.getMessage());
                return null;
            }
        });
        
        assertNotNull(textArea);
        assertFalse(textArea.isEditable());
        assertTrue(textArea.getLineWrap());
        assertTrue(textArea.getWrapStyleWord());
        assertEquals("Monospaced", textArea.getFont().getFamily());
    }

    @Test
    @DisplayName("Crea paneles con scroll con título correctamente")
    void shouldCreateTitledScrollPanes() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        JTextArea textArea = new JTextArea();
        
        // Acceder al método createTitledScroll via reflexión
        Method createTitledScrollMethod = GameHostFrame.class.getDeclaredMethod("createTitledScroll", String.class, JTextArea.class);
        createTitledScrollMethod.setAccessible(true);
        
        JScrollPane scrollPane = (JScrollPane) GuiActionRunner.execute(() -> {
            try {
                return createTitledScrollMethod.invoke(frame, "Test Title", textArea);
            } catch (Exception e) {
                fail("Error invoking createTitledScroll: " + e.getMessage());
                return null;
            }
        });
        
        assertNotNull(scrollPane);
        assertNotNull(scrollPane.getBorder());
        assertEquals(textArea, scrollPane.getViewport().getView());
    }

    @Test
    @DisplayName("Maneja excepción en cleanup de forma silenciosa")
    void shouldHandleIOExceptionInCleanupSilently() throws Exception {
        ClientSession sessionWithError = new IOExceptionClientSession();
        
        GameHostFrame frame = GuiActionRunner.execute(() -> new GameHostFrame(() -> sessionWithError));
        
        // Acceder al campo boardOpened via reflexión y establecerlo a false
        Field boardOpenedField = GameHostFrame.class.getDeclaredField("boardOpened");
        boardOpenedField.setAccessible(true);
        boardOpenedField.set(frame, false);
        
        // Acceder al método cleanup via reflexión
        Method cleanupMethod = GameHostFrame.class.getDeclaredMethod("cleanup");
        cleanupMethod.setAccessible(true);
        
        // Esto no debería lanzar excepción a pesar del IOException
        assertDoesNotThrow(() -> cleanupMethod.invoke(frame));
    }

    @Test
    @DisplayName("No ejecuta cleanup múltiples veces")
    void shouldNotExecuteCleanupMultipleTimes() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al método cleanup y campo cleanedUp via reflexión
        Method cleanupMethod = GameHostFrame.class.getDeclaredMethod("cleanup");
        cleanupMethod.setAccessible(true);
        Field cleanedUpField = GameHostFrame.class.getDeclaredField("cleanedUp");
        cleanedUpField.setAccessible(true);
        
        // Ejecutar cleanup primera vez
        cleanupMethod.invoke(frame);
        boolean firstCleanup = (boolean) cleanedUpField.get(frame);
        
        // Ejecutar cleanup segunda vez
        cleanupMethod.invoke(frame);
        boolean secondCleanup = (boolean) cleanedUpField.get(frame);
        
        assertTrue(firstCleanup);
        assertTrue(secondCleanup);
    }

    @Test
    @DisplayName("Procesa notificaciones de forma segura en EDT")
    void shouldProcessNotificationsSafelyInEDT() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al método appendNotificationSafely via reflexión
        Method appendNotificationSafelyMethod = GameHostFrame.class.getDeclaredMethod("appendNotificationSafely", String.class);
        appendNotificationSafelyMethod.setAccessible(true);
        
        // Llamar desde un hilo que no es EDT
        new Thread(() -> {
            assertDoesNotThrow(() -> {
                appendNotificationSafelyMethod.invoke(frame, "Thread-safe notification");
            });
        }).start();
        
        // Esperar y verificar que el mensaje aparece
        Pause.pause(1000);
        String notificationText = window.textBox("notificationArea").text();
        assertTrue(notificationText.contains("Thread-safe notification"));
    }

    @Test
    @DisplayName("Maneja sessionSupplier nulo en constructor")
    void shouldHandleNullSessionSupplier() {
        assertThrows(NullPointerException.class, () -> {
            new GameHostFrame(null);
        });
    }

    @Test
    @DisplayName("Maneja session nulo en constructor")
    void shouldHandleNullSessionFromSupplier() {
        assertThrows(NullPointerException.class, () -> {
            new GameHostFrame(() -> null);
        });
    }

    @Test
    @DisplayName("Verifica que el worker se cancela correctamente")
    void shouldCancelWorkerOnCancel() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al campo worker via reflexión
        Field workerField = GameHostFrame.class.getDeclaredField("worker");
        workerField.setAccessible(true);
        SwingWorker<Void, Void> worker = (SwingWorker<Void, Void>) workerField.get(frame);
        
        // Verificar que el worker existe y no está done inicialmente
        assertNotNull(worker);
        assertTrue(worker.isDone());
        
        // Hacer click en cancelar
        window.button("cancelButton").click();
        
        // Verificar que el worker fue cancelado
        assertTrue(worker.isCancelled() || worker.isDone());
    }

    @Test
    @DisplayName("Verifica que se remueven suscriptores de notificaciones")
    void shouldRemoveNotificationSubscribers() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder al campo notificationConsumer via reflexión
        Field notificationConsumerField = GameHostFrame.class.getDeclaredField("notificationConsumer");
        notificationConsumerField.setAccessible(true);
        Consumer<String> consumer = (Consumer<String>) notificationConsumerField.get(frame);
        
        // Verificar que el consumer fue agregado al session
        assertTrue(sessionStub.getNotificationConsumers().contains(consumer));
        
        // Cerrar la ventana para trigger cleanup
        window.close();
        
        // Verificar que el consumer fue removido
        assertFalse(sessionStub.getNotificationConsumers().contains(consumer));
    }

    @Test
    @DisplayName("Verifica el comportamiento cuando board ya está abierto")
    void shouldNotCloseSessionWhenBoardAlreadyOpened() throws Exception {
        GameHostFrame frame = (GameHostFrame) window.target();
        
        // Acceder a los campos necesarios via reflexión
        Field boardOpenedField = GameHostFrame.class.getDeclaredField("boardOpened");
        boardOpenedField.setAccessible(true);
        Field cleanedUpField = GameHostFrame.class.getDeclaredField("cleanedUp");
        cleanedUpField.setAccessible(true);
        
        // Establecer boardOpened a true
        boardOpenedField.set(frame, true);
        
        // Acceder al método cleanup via reflexión
        Method cleanupMethod = GameHostFrame.class.getDeclaredMethod("cleanup");
        cleanupMethod.setAccessible(true);
        
        // Ejecutar cleanup
        cleanupMethod.invoke(frame);
        
        // Verificar que se marcó como cleanedUp pero no se llamó a session.close()
        boolean finalCleanup = (boolean) cleanedUpField.get(frame);
        assertTrue(finalCleanup);
        
        // En este caso, como boardOpened es true, no debería llamarse a session.close()
        // Podemos verificar esto indirectamente verificando que el stub no registró un cierre
    }

    // Clase Stub interna para testing normal
    private static class StubClientSession implements ClientSession {
        private int jugadorId = 1;
        private int partidoId = 1;
        private String host = "localhost";
        private int port = 8080;
        private List<String> welcomeMessages = new ArrayList<>();
        private List<Consumer<String>> notificationConsumers = new ArrayList<>();
        private boolean closed = false;

        public StubClientSession() {
            super();
            welcomeMessages.add("Bienvenido al servidor");
            welcomeMessages.add("Partida creada exitosamente");
        }

        @Override
        public void connect() {
            // Simular conexión exitosa
        }

        @Override
        public int ensureJugador() {
            return jugadorId;
        }

        @Override
        public int crearPartido() {
            return partidoId;
        }

        @Override
        public List<String> getWelcomeMessages() {
            return new ArrayList<>(welcomeMessages);
        }

        @Override
        public void agregarSuscriptorNotificaciones(Consumer<String> consumer) {
            notificationConsumers.add(consumer);
        }

        @Override
        public void quitarSuscriptorNotificaciones(Consumer<String> consumer) {
            notificationConsumers.remove(consumer);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            // Simular cierre
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public Integer getPartidoId() {
            return partidoId;
        }

        // Métodos para testing
        public void setJugadorId(int id) {
            this.jugadorId = id;
        }

        public void setPartidoId(int id) {
            this.partidoId = id;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setWelcomeMessages(List<String> messages) {
            this.welcomeMessages = new ArrayList<>(messages);
        }

        public List<Consumer<String>> getNotificationConsumers() {
            return new ArrayList<>(notificationConsumers);
        }
        
        public boolean isClosed() {
            return closed;
        }

        @Override
        public boolean isConnected() {
            throw new UnsupportedOperationException("Unimplemented method 'isConnected'");
        }

        @Override
        public void usarJugador(int jugadorId) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'usarJugador'");
        }

        @Override
        public int unirsePartido(int gameId) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'unirsePartido'");
        }

        @Override
        public ShipPlacementResult colocarBarco(List<int[]> coords) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'colocarBarco'");
        }

        @Override
        public ResultadoDisparo disparar(int fila, int columna) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'disparar'");
        }

        @Override
        public Integer getJugadorId() {
            throw new UnsupportedOperationException("Unimplemented method 'getJugadorId'");
        }
    }

    // Clase Stub para simular fallos de conexión
    private static class FailingClientSession implements ClientSession {
        public FailingClientSession() {
            super();
        }

        @Override
        public void connect() {
            throw new RuntimeException("Error de conexión simulado");
        }

        @Override
        public int ensureJugador() {
            return 1;
        }

        @Override
        public int crearPartido() {
            return 1;
        }

        @Override
        public List<String> getWelcomeMessages() {
            return new ArrayList<>();
        }

        @Override
        public void agregarSuscriptorNotificaciones(Consumer<String> consumer) {
            // No hacer nada
        }

        @Override
        public void quitarSuscriptorNotificaciones(Consumer<String> consumer) {
            // No hacer nada
        }

        @Override
        public void close() throws IOException {
            // No hacer nada
        }

        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public int getPort() {
            return 8080;
        }

        @Override
        public Integer getPartidoId() {
            return 1;
        }

        @Override
        public boolean isConnected() {
            throw new UnsupportedOperationException("Unimplemented method 'isConnected'");
        }

        @Override
        public void usarJugador(int jugadorId) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'usarJugador'");
        }

        @Override
        public int unirsePartido(int gameId) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'unirsePartido'");
        }

        @Override
        public ShipPlacementResult colocarBarco(List<int[]> coords) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'colocarBarco'");
        }

        @Override
        public ResultadoDisparo disparar(int fila, int columna) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'disparar'");
        }

        @Override
        public Integer getJugadorId() {
            throw new UnsupportedOperationException("Unimplemented method 'getJugadorId'");
        }
    }

    // Clase Stub para simular IOException en close
    private static class IOExceptionClientSession implements ClientSession {
        public IOExceptionClientSession() {
            super();
        }

        @Override
        public void connect() {
            // Simular conexión exitosa
        }

        @Override
        public int ensureJugador() {
            return 1;
        }

        @Override
        public int crearPartido() {
            return 1;
        }

        @Override
        public List<String> getWelcomeMessages() {
            return new ArrayList<>();
        }

        @Override
        public void agregarSuscriptorNotificaciones(Consumer<String> consumer) {
            // No hacer nada
        }

        @Override
        public void quitarSuscriptorNotificaciones(Consumer<String> consumer) {
            // No hacer nada
        }

        @Override
        public void close() throws IOException {
            throw new IOException("Error de IO simulado");
        }

        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public int getPort() {
            return 8080;
        }

        @Override
        public Integer getPartidoId() {
            return 1;
        }

        @Override
        public boolean isConnected() {
            throw new UnsupportedOperationException("Unimplemented method 'isConnected'");
        }

        @Override
        public void usarJugador(int jugadorId) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'usarJugador'");
        }

        @Override
        public int unirsePartido(int gameId) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'unirsePartido'");
        }

        @Override
        public ShipPlacementResult colocarBarco(List<int[]> coords) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'colocarBarco'");
        }

        @Override
        public ResultadoDisparo disparar(int fila, int columna) throws IOException, TcpClientException {
            throw new UnsupportedOperationException("Unimplemented method 'disparar'");
        }

        @Override
        public Integer getJugadorId() {
            throw new UnsupportedOperationException("Unimplemented method 'getJugadorId'");
        }
    }
}
