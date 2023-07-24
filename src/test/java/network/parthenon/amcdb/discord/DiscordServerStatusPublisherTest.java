package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.messaging.message.ServerStatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

class DiscordServerStatusPublisherTest {
    DiscordService mockDiscordService;

    DiscordConfig mockConfig;

    @BeforeEach
    public void setUp() {
        mockDiscordService = Mockito.mock(DiscordService.class);
        mockConfig = Mockito.mock(DiscordConfig.class);
    }

    /**
     * Tests that data in a server status message is published to the Discord channel topics.
     */
    @Test
    public void testTopicUpdate() {
        Mockito.when(mockConfig.getDiscordChatTopicFormat()).thenReturn(Optional.of("%playersOnline%/%maxPlayers% players online"));
        Mockito.when(mockConfig.getDiscordConsoleTopicFormat()).thenReturn(Optional.of("MSPT: %mspt%"));
        DiscordServerStatusPublisher publisher = new DiscordServerStatusPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 18.0, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        Mockito.verify(mockDiscordService).setChatChannelTopic("2/10 players online");
        Mockito.verify(mockDiscordService).setConsoleChannelTopic("MSPT\\: 18.0");
    }

    /**
     * Tests that Discord channel topic updates are not sent when the channel topic
     * feature is disabled.
     */
    @Test
    public void testTopicUpdateDisabled() {
        Mockito.when(mockConfig.getDiscordChatTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordConsoleTopicFormat()).thenReturn(Optional.empty());
        DiscordServerStatusPublisher publisher = new DiscordServerStatusPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 18.0, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        Mockito.verify(mockDiscordService, Mockito.never()).setChatChannelTopic(Mockito.anyString());
        Mockito.verify(mockDiscordService, Mockito.never()).setConsoleChannelTopic(Mockito.anyString());
    }

    /**
     * Tests that the MSPT alert is sent when two consecutive MSPT values are above
     * the configured threshold.
     */
    @Test
    public void testMsptAlert() {
        Mockito.when(mockConfig.getDiscordChatTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordConsoleTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordAlertMsptThreshold()).thenReturn(OptionalLong.of(50));
        Mockito.when(mockConfig.getDiscordAlertCooldown()).thenReturn(600L);
        DiscordServerStatusPublisher publisher = new DiscordServerStatusPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.1, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.2, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        Mockito.verify(mockDiscordService, Mockito.times(1))
                .sendToConsoleChannel("@everyone MSPT is 50.2 (configured alert threshold is 50)");
    }

    /**
     * Tests that the MSPT alert is not sent when only a single, isolated MSPT value
     * is above the configured threshold.
     */
    @Test
    public void testMsptSpike() {
        Mockito.when(mockConfig.getDiscordChatTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordConsoleTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordAlertMsptThreshold()).thenReturn(OptionalLong.of(50));
        Mockito.when(mockConfig.getDiscordAlertCooldown()).thenReturn(600L);
        DiscordServerStatusPublisher publisher = new DiscordServerStatusPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.1, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 19.0, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that only one MSPT alert is sent when MSPT is sustained above the threshold.
     */
    @Test
    public void testMsptAlertCooldown() {
        Mockito.when(mockConfig.getDiscordChatTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordConsoleTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordAlertMsptThreshold()).thenReturn(OptionalLong.of(50));
        Mockito.when(mockConfig.getDiscordAlertCooldown()).thenReturn(600L);
        DiscordServerStatusPublisher publisher = new DiscordServerStatusPublisher(mockDiscordService, mockConfig);

        for(int i = 0; i < 5; i++) {
            publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.2, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        }
        Mockito.verify(mockDiscordService, Mockito.times(1))
                .sendToConsoleChannel("@everyone MSPT is 50.2 (configured alert threshold is 50)");
    }

    /**
     * Tests that MSPT alerts are not sent when this feature is disabled.
     */
    @Test
    public void testMsptAlertDisabled() {
        Mockito.when(mockConfig.getDiscordChatTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordConsoleTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordAlertMsptThreshold()).thenReturn(OptionalLong.empty());
        Mockito.when(mockConfig.getDiscordAlertCooldown()).thenReturn(600L);
        DiscordServerStatusPublisher publisher = new DiscordServerStatusPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.1, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.2, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        Mockito.verify(mockDiscordService, Mockito.never()).sendToConsoleChannel(Mockito.anyString());
    }

    /**
     * Tests that specific users and roles are tagged in an MSPT alert,
     * when specified.
     */
    @Test
    public void testMsptAlertTags() {
        Mockito.when(mockConfig.getDiscordChatTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordConsoleTopicFormat()).thenReturn(Optional.empty());
        Mockito.when(mockConfig.getDiscordAlertMsptThreshold()).thenReturn(OptionalLong.of(50));
        Mockito.when(mockConfig.getDiscordAlertUserIds()).thenReturn(Optional.of(List.of(1000L, 2000L)));
        Mockito.when(mockConfig.getDiscordAlertRoleIds()).thenReturn(Optional.of(List.of(3000L)));
        Mockito.when(mockConfig.getDiscordAlertCooldown()).thenReturn(600L);
        DiscordServerStatusPublisher publisher = new DiscordServerStatusPublisher(mockDiscordService, mockConfig);

        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.1, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        publisher.handleMessage(new ServerStatusMessage("JUNIT_TEST_SOURCE_ID", 50.2, 4096, 1024, 2, 10, List.of(new TextComponent("A Minecraft Server")), System.currentTimeMillis()));
        Mockito.verify(mockDiscordService, Mockito.times(1))
                .sendToConsoleChannel("<@1000> <@2000> <@&3000> MSPT is 50.2 (configured alert threshold is 50)");
    }
}
