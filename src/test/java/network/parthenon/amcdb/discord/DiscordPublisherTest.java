package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.PlayerConnectionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DiscordPublisherTest {

    DiscordService mockDiscordService;

    RoleManager mockRoleManager;

    DiscordConfig mockConfig;

    @BeforeEach
    public void setUp() {
        mockDiscordService = Mockito.mock(DiscordService.class);
        mockRoleManager = Mockito.mock(RoleManager.class);
        mockConfig = Mockito.mock(DiscordConfig.class);
    }

    /**
     * Tests that when webhook mode is not enabled, chat messages are sent to the chat channel
     * via the bot (not the webhook) and not to the console channel.
     */
    @Test
    public void testChatMessage() {
        setupConfig(true, false, true);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);

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

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);

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

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);

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

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);

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

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);

        publisher.handleMessage(new ConsoleMessage("JUNIT_TEST_SOURCE_ID", null, "test message"));

        Mockito.verify(mockDiscordService).sendToConsoleChannel("test message");
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatWebhook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Tests that no messages are sent when the chat and console channels are disabled.
     */
    @Test
    public void testDisabledChannels() {
        setupConfig(false, false, false);

        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);

        publisher.handleMessage(new ChatMessage("JUNIT_TEST_SOURCE_ID", new EntityReference("authorId"), "test message"));
        publisher.handleMessage(new BroadcastMessage("JUNIT_TEST_SOURCE_ID", "test message"));
        publisher.handleMessage(new ConsoleMessage("JUNIT_TEST_SOURCE_ID", null, "test message"));

        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatChannel(Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToChatWebhook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that the RoleManager is informed when a player join message is received.
     */
    @Test
    public void testPlayerJoin() {
        UUID playerUuid = UUID.randomUUID();
        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);
        publisher.handleMessage(PlayerConnectionMessage.join(new EntityReference(playerUuid.toString())));

        Mockito.verify(mockRoleManager, Mockito.times(1)).updateOnlineRole(playerUuid, true);
    }

    /**
     * Tests that the RoleManager is informed when a player leave message is received.
     */
    @Test
    public void testPlayerLeave() {
        UUID playerUuid = UUID.randomUUID();
        DiscordPublisher publisher = new DiscordPublisher(mockDiscordService, mockRoleManager, mockConfig);
        publisher.handleMessage(PlayerConnectionMessage.leave(new EntityReference(playerUuid.toString())));

        Mockito.verify(mockRoleManager, Mockito.times(1)).updateOnlineRole(playerUuid, false);
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
