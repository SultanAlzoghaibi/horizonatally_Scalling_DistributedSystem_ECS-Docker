import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class GameServer2STTest {

    @Test
    void testServerSocketInitialization() {
        try (ServerSocket serverSocket = new ServerSocket(30002)) {
            assertNotNull(serverSocket);
        } catch (IOException e) {
            fail("Server socket failed to initialize: " + e.getMessage());
        }
    }

    @Test
    void testServerSocketBindsToFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int assignedPort = serverSocket.getLocalPort();
            assertTrue(assignedPort > 0);
        } catch (IOException e) {
            fail("Could not bind to a free port: " + e.getMessage());
        }
    }
}