import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import server.Server;
import common.Topic;
import java.util.Map;

class ServerTest {

    @BeforeEach
    void setUp() {
        Server.getTopics().clear();
        Server.getActiveUsers().clear();
    }

    @Test
    void testLoginUser() {
        assertTrue(Server.loginUser("zhenya"));
        assertFalse(Server.loginUser("zhenya"));
    }

    @Test
    void testLogoutUser() {
        Server.loginUser("zhenya");
        assertEquals(1, Server.getActiveUsers().size());
        Server.logoutUser("zhenya");
        assertEquals(0, Server.getActiveUsers().size());
    }

    @Test
    void testCreateAndRetrieveTopic() {
        Map<String, Topic> topics = Server.getTopics();
        assertTrue(topics.isEmpty());
        topics.put("testTopic", new Topic("testTopic"));
        assertEquals(1, topics.size());
        assertTrue(topics.containsKey("testTopic"));
    }

    @Test
    void testSaveAndLoad() {
        String filename = "testdata.json";
        Server.getTopics().put("testTopic", new Topic("testTopic"));
        Server.save(filename);

        Server.getTopics().clear();
        assertTrue(Server.getTopics().isEmpty());

        Server.load(filename);
        assertFalse(Server.getTopics().isEmpty());
        assertTrue(Server.getTopics().containsKey("testTopic"));
    }
}