package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.discord.JDAMocks.MockChannel;
import network.parthenon.amcdb.discord.JDAMocks.MockSelfUser;
import network.parthenon.amcdb.discord.JDAMocks.MockUser;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class DiscordListenerTest {

    MessageBroker mockBroker;

    DiscordService mockDiscordService;

    JDA mockJDA;

    @BeforeEach
    public void setUp() {
        mockBroker = Mockito.mock(MessageBroker.class);
        mockDiscordService = Mockito.mock(DiscordService.class);
        mockJDA = mockJDA();
    }

    /**
     * Tests that a received chat message is published to the message broker.
     */
    @Test
    public void chatMessage() {
        DiscordConfig config = Mockito.mock(DiscordConfig.class);
        Mockito.when(config.getDiscordChatChannel()).thenReturn(OptionalLong.of(1234));
        Mockito.when(config.getDiscordConsoleChannel()).thenReturn(OptionalLong.of(2345));

        DiscordListener listener = new DiscordListener(mockDiscordService, config, mockBroker);

        listener.onMessageReceived(mockMessageReceivedEvent("test message", 1234, 555));

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        Mockito.verify(mockBroker).publish(messageCaptor.capture());
        assertEquals("test message", messageCaptor.getValue().getUnformattedContents());
        assertEquals("555", messageCaptor.getValue().getAuthor().getEntityId());
    }

    /**
     * Tests that a received console message is published to the message broker
     * when console execution is enabled.
     */
    @Test
    public void consoleMessageExecutionEnabled() {
        DiscordConfig config = Mockito.mock(DiscordConfig.class);
        Mockito.when(config.getDiscordChatChannel()).thenReturn(OptionalLong.of(1234));
        Mockito.when(config.getDiscordConsoleChannel()).thenReturn(OptionalLong.of(2345));
        Mockito.when(config.getDiscordConsoleExecutionEnabled()).thenReturn(true);

        DiscordListener listener = new DiscordListener(mockDiscordService, config, mockBroker);

        listener.onMessageReceived(mockMessageReceivedEvent("test command", 2345, 555));

        ArgumentCaptor<ConsoleMessage> messageCaptor = ArgumentCaptor.forClass(ConsoleMessage.class);
        Mockito.verify(mockBroker).publish(messageCaptor.capture());
        assertEquals("test command", messageCaptor.getValue().getUnformattedContents());
        assertEquals("555", messageCaptor.getValue().getAuthor().getEntityId());
    }

    /**
     * Tests that a received console message is *not* published to the message broker
     * when console execution is disabled, and that a warning is sent to the console channel.
     */
    @Test
    public void consoleMessageExecutionDisabled() {
        DiscordConfig config = Mockito.mock(DiscordConfig.class);
        Mockito.when(config.getDiscordChatChannel()).thenReturn(OptionalLong.of(1234));
        Mockito.when(config.getDiscordConsoleChannel()).thenReturn(OptionalLong.of(2345));
        Mockito.when(config.getDiscordConsoleExecutionEnabled()).thenReturn(false);

        DiscordListener listener = new DiscordListener(mockDiscordService, config, mockBroker);

        listener.onMessageReceived(mockMessageReceivedEvent("test command", 2345, 555));

        Mockito.verify(mockBroker, Mockito.never()).publish(Mockito.any());
        Mockito.verify(mockDiscordService).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Creates a mock JDA instance.
     * @return
     */
    private JDA mockJDA() {
        JDA mockJDA = Mockito.mock(JDA.class);
        Mockito.when(mockJDA.getSelfUser()).thenReturn(new MockSelfUser());
        return mockJDA;
    }

    /**
     * Creates a MessageReceivedEvent containing a message with the
     * specified content and author.
     * @param message  The message contents.
     * @param authorId The author ID to set.
     * @return
     */
    private MessageReceivedEvent mockMessageReceivedEvent(String message, long channelId, long authorId) {
        Message mockMessage = Mockito.mock(Message.class);
        Mockito.when(mockMessage.getChannel()).thenReturn(new MockChannel(channelId));
        Mockito.when(mockMessage.getAuthor()).thenReturn(new MockUser(authorId));
        Mockito.when(mockMessage.getContentRaw()).thenReturn(message);

        return new MessageReceivedEvent(mockJDA, 1L, mockMessage);
    }
}