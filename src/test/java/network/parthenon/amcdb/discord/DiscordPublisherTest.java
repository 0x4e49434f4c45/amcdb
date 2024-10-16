package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.ServerLifecycleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DiscordPublisherTest {

    DiscordService mockDiscordService;

    DiscordConfig mockConfig;

    @BeforeEach
    public void setUp() {
        mockDiscordService = Mockito.mock(DiscordService.class);
        mockConfig = Mockito.mock(DiscordConfig.class);
    }

    /**
     * Tests that when webhook mode is not enabled, chat messages are sent to the chat channel
     * via the bot (not the webhook) and not to the console channel.
     */
    @Test
    public void testChatMessage() {
        setupConfig(true, false, true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("authorId"), "test message"));

        Mockito.verify(mockDiscordService).sendToChatChannel("\\<authorId\\> test message");
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatWebhook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that when webhook mode is enabled, chat messages are sent to the chat channel
     * via the webhook with the proper username and avatar URL set, and are not sent to the
     * console channel.
     */
    @Test
    public void testWebhookChatMessage() {
        setupConfig(true, true, true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ChatMessage(
                "JUNIT_TEST_SOURCE_ID",
                new EntityReference("authorId", "authorDisplayName", null, null, EnumSet.noneOf(InternalMessageComponent.Style.class), "https://fake.avatar.url/"),
                "test message"));

        Mockito.verify(mockDiscordService).sendToChatWebhook("test message", "authorDisplayName", "https://fake.avatar.url/");
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that when webhook mode is not enabled, broadcast messages are sent to the chat channel
     * via the bot (not the webhook) and not to the console channel.
     */
    @Test
    public void testBroadcastMessage() {
        setupConfig(true, false, true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new BroadcastMessage("JUNIT_TEST_SOURCE_ID", "test message"));

        Mockito.verify(mockDiscordService).sendToChatChannel("test message");
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatWebhook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that when webhook mode is enabled, broadcast messages are sent to the chat channel
     * via the webhook with the default username and avatar URL set, and are not sent to the
     * console channel.
     */
    @Test
    public void testWebhookBroadcastMessage() {
        setupConfig(true, true, true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new BroadcastMessage("JUNIT_TEST_SOURCE_ID", "test message"));

        Mockito.verify(mockDiscordService).sendToChatWebhook("test message", null, null);
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that a console message is sent to the console channel and not to the chat channel.
     */
    @Test
    public void testConsoleMessage() {
        setupConfig(true, false, true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ConsoleMessage("JUNIT_TEST_SOURCE_ID", null, "test message"));

        Mockito.verify(mockDiscordService).sendToConsoleChannel("test message");
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatWebhook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }


    /**
     * Tests that lifecycle messages are properly published to the chat channel.
     */
    @Test
    public void testLifecycleMessage() {
        setupConfig(true, false, false);
        Mockito.when(mockConfig.getDiscordLifecycleStartedFormat()).thenReturn(Optional.of("Custom: Server started!"));
        Mockito.when(mockConfig.getDiscordLifecycleStoppedFormat()).thenReturn(Optional.of("Custom: Server stopped!"));

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(ServerLifecycleMessage.started("JUNIT_TEST_SOURCE_ID"));
        Mockito.verify(mockDiscordService).sendToChatChannel("Custom\\: Server started!");

        publisher.handleMessage(ServerLifecycleMessage.stopped("JUNIT_TEST_SOURCE_ID"));
        Mockito.verify(mockDiscordService).sendToChatChannel("Custom\\: Server stopped!");
    }

    /**
     * Tests that lifecycle messages are not published when their formats are disabled.
     */
    @Test
    public void testLifecycleMessageDisabled() {
        setupConfig(true, false, false);
        Mockito.when(mockConfig.getDiscordLifecycleStartedFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordLifecycleStoppedFormat()).thenReturn(Optional.empty());

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(ServerLifecycleMessage.started("JUNIT_TEST_SOURCE_ID"));
        publisher.handleMessage(ServerLifecycleMessage.stopped("JUNIT_TEST_SOURCE_ID"));
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
    }

    /**
     * Tests that no messages are sent when the chat and console channels are disabled.
     */
    @Test
    public void testDisabledChannels() {
        setupConfig(false, false, false);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("authorId"), "test message"));
        publisher.handleMessage(new BroadcastMessage("JUNIT_TEST_SOURCE_ID", "test message"));
        publisher.handleMessage(new ConsoleMessage("JUNIT_TEST_SOURCE_ID", null, "test message"));

        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatWebhook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that messages are not sent when the message filter pattern is configured to exclude them.
     */
    @Test
    public void testExcludeMessageFilter() {
        setupConfig(true, false, true);

        Mockito.when(mockConfig.getDiscordMessageFilterPattern()).thenReturn(Optional.of(Pattern.compile("^IgnoreThis")));
        Mockito.when(mockConfig.getDiscordMessageFilterExclude()).thenReturn(true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("authorId"), "test message"));
        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("authorId"), "IgnoreThis test message"));
        publisher.handleMessage(new BroadcastMessage("JUNIT_TEST_SOURCE_ID", "IgnoreThis test broadcast message"));

        // it should send "test message"...
        Mockito.verify(mockDiscordService, Mockito.times(1)).sendToChatChannel("\\<authorId\\> test message");
        // ...and *only* "test message"
        Mockito.verify(mockDiscordService, Mockito.times(1)).sendToChatChannel(Mockito.anyString());
    }

    /**
     * Tests that only matching messages are sent when the message filter pattern is configured in include mode.
     */
    @Test
    public void testIncludeMessageFilter() {
        setupConfig(true, false, true);

        Mockito.when(mockConfig.getDiscordMessageFilterPattern()).thenReturn(Optional.of(Pattern.compile("^IncludeThis")));
        Mockito.when(mockConfig.getDiscordMessageFilterExclude()).thenReturn(false);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("authorId"), "test message"));
        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("authorId"), "IncludeThis test message"));
        publisher.handleMessage(new BroadcastMessage("JUNIT_TEST_SOURCE_ID", "test broadcast message"));

        // it should send "IncludeThis test message"...
        Mockito.verify(mockDiscordService, Mockito.times(1)).sendToChatChannel("\\<authorId\\> IncludeThis test message");
        // ...and *only* "IncludeThis test message"
        Mockito.verify(mockDiscordService, Mockito.times(1)).sendToChatChannel(Mockito.anyString());
    }

    /**
     * Tests that messages are not sent when the sender is on the ignore list.
     */
    @Test
    public void testExcludeUsers() {
        setupConfig(true, false, true);

        Mockito.when(mockConfig.getDiscordIgnoredExternalUsers()).thenReturn(Optional.of(List.of("0x4e49434f4c45")));

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("0xId", "0x", "0x4e49434f4c45"), "test message from 0x"));
        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("someoneElseId", "SomeoneElse", "someoneElse"), "test message from someone else"));

        // it should send the message from SomeoneElse...
        Mockito.verify(mockDiscordService, Mockito.times(1)).sendToChatChannel("\\<SomeoneElse\\> test message from someone else");
        // ...and *only* the message from SomeoneElse
        Mockito.verify(mockDiscordService, Mockito.times(1)).sendToChatChannel(Mockito.anyString());
    }

    /**
     * Tests that broadcast messages are not sent if ignored.
     */
    @Test
    public void testIgnoreBroadcast() {
        setupConfig(true, false, true);

        Mockito.when(mockConfig.getDiscordIgnoreBroadcast()).thenReturn(true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new BroadcastMessage("JUNIT_TEST_SOURCE_ID", "test broadcast message"));

        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
    }

    private void setupConfig(boolean isChatChannelEnabled, boolean isChatWebhookEnabled, boolean isConsoleChannelEnabled) {
        Mockito.when(mockDiscordService.isChatChannelEnabled()).thenReturn(isChatChannelEnabled);
        Mockito.when(mockDiscordService.isChatWebhookEnabled()).thenReturn(isChatWebhookEnabled);
        Mockito.when(mockDiscordService.isConsoleChannelEnabled()).thenReturn(isConsoleChannelEnabled);

        Mockito.when(mockConfig.getDiscordChatMessageFormat()).thenReturn("<%username%> %message%");
        Mockito.when(mockConfig.getDiscordWebhookChatMessageFormat()).thenReturn("%message%");
        Mockito.when(mockConfig.getDiscordBroadcastMessageFormat()).thenReturn("%message%");
    }
}
