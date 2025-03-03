import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.ServerHandler;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import server.Server;
import common.Topic;
import common.Vote;
import java.util.Map;
import java.util.Set;

class ServerHandlerTest {
    private TestableServerHandler handler;
    private ChannelHandlerContext mockCtx;

    @BeforeEach
    void setUp() {
        handler = new TestableServerHandler();
        mockCtx = mock(ChannelHandlerContext.class);
        Server.getTopics().clear();
        Server.getActiveUsers().clear();
    }

    @Test
    void testHandleLogin() {
        handler.processCommand(mockCtx, "login -u=zhenya");
        verify(mockCtx, times(1)).writeAndFlush(contains("Вы вошли под пользователем zhenya"));
    }

    @Test
    void testHandleCreateTopic() {
        handler.processCommand(mockCtx, "login -u=zhenya");
        handler.processCommand(mockCtx, "create topic -t=psychology");
        verify(mockCtx, times(1)).writeAndFlush(contains("Создан раздел голосования: psychology"));
    }

    @Test
    void testHandleViewTopics() {
        handler.processCommand(mockCtx, "login -u=zhenya");
        handler.processCommand(mockCtx, "create topic -t=polls");
        handler.processCommand(mockCtx, "view");
        verify(mockCtx, times(1)).writeAndFlush(contains("Текущий список разделов"));
    }

    @Test
    void testHandleVote() {
        handler.processCommand(mockCtx, "login -u=zhenya");
        handler.processCommand(mockCtx, "create topic -t=basic");
        handler.processCommand(mockCtx, "create vote -t=basic");
        handler.processCommand(mockCtx, "animals");
        handler.processCommand(mockCtx, "Are dogs mammals?");
        handler.processCommand(mockCtx, "2");
        handler.processCommand(mockCtx, "Yes");
        handler.processCommand(mockCtx, "No");
        handler.processCommand(mockCtx, "vote -t=basic -v=animals");
        verify(mockCtx, atLeastOnce()).writeAndFlush(contains("Вы перешли к голосованию"));
    }

    @Test
    void testHandleDeleteVote() {
        handler.processCommand(mockCtx, "login -u=zhenya");
        handler.processCommand(mockCtx, "create topic -t=testTopic");
        handler.processCommand(mockCtx, "create vote -t=testTopic");
        handler.processCommand(mockCtx, "testVote");
        handler.processCommand(mockCtx, "testQuestion");
        handler.processCommand(mockCtx, "3");
        handler.processCommand(mockCtx, "answer1");
        handler.processCommand(mockCtx, "answer2");
        handler.processCommand(mockCtx, "answer3");
        handler.processCommand(mockCtx, "delete -t=testTopic -v=testVote");
        verify(mockCtx, atLeastOnce()).writeAndFlush(contains("Голосование testVote было удалено"));
    }

    @Test
    void testHandleSaveAndLoad() {
        handler.processCommand(mockCtx, "login -u=zhenya");
        handler.processCommand(mockCtx, "create topic -t=testTopic");
        handler.processCommand(mockCtx, "create vote -t=testTopic");
        handler.processCommand(mockCtx, "testVote");
        handler.processCommand(mockCtx, "testQuestion");
        handler.processCommand(mockCtx, "3");
        handler.processCommand(mockCtx, "answer1");
        handler.processCommand(mockCtx, "answer2");
        handler.processCommand(mockCtx, "answer3");
        handler.processCommand(mockCtx, "save testdata.json");
        verify(mockCtx, atLeastOnce()).writeAndFlush(contains("Данные успешно сохранены"));
        handler.processCommand(mockCtx, "load testdata.json");
        verify(mockCtx, atLeastOnce()).writeAndFlush(contains("Данные успешно загружены"));
    }

    @Test
    void testHandleInvalidCommand() {
        handler.processCommand(mockCtx, "afkaefjksmerk");
        verify(mockCtx, times(1)).writeAndFlush(contains("Введена несуществующая команда"));
    }

    private static class TestableServerHandler extends ServerHandler { // получаем обработчик команд
        void processCommand(ChannelHandlerContext ctx, String msg) {
            channelRead0(ctx, msg);
        }
    }
}