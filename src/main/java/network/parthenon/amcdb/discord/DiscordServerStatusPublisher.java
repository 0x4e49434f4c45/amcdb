package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.MessageHandler;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.message.ServerStatusMessage;

import java.util.List;
import java.util.function.Consumer;

public class DiscordServerStatusPublisher implements MessageHandler {
    private final DiscordService discordService;

    private final DiscordConfig config;

    private final DiscordFormatter formatter;

    private long lastTopicUpdateTime = 0;

    private long lastMsptAlertTime = 0;

    private double lastMsptValue = 0;

    public DiscordServerStatusPublisher(DiscordService discordService, DiscordConfig config) {
        this.discordService = discordService;
        this.config = config;
        this.formatter = new DiscordFormatter(discordService, config);
    }

    @Override
    public void handleMessage(InternalMessage message) {
        if(!(message instanceof ServerStatusMessage)) {
            return;
        }
        ServerStatusMessage statusMessage = (ServerStatusMessage) message;

        AMCDB.LOGGER.debug(statusMessage.toString());

        long now = System.currentTimeMillis();

        if(now - lastTopicUpdateTime > config.getDiscordTopicUpdateInterval() * 1000) {
            lastTopicUpdateTime = now;

            if(config.getDiscordChatTopicFormat().isPresent()) {
                List<String> topicChunks = formatter.toDiscordRawContent(
                        statusMessage.formatToComponents(config.getDiscordChatTopicFormat().orElseThrow()).stream(),
                        DiscordService.DISCORD_TOPIC_CHAR_LIMIT
                );
                if(topicChunks.size() > 0) {
                    discordService.setChatChannelTopic(topicChunks.get(0));
                }
            }

            if(config.getDiscordConsoleTopicFormat().isPresent()) {
                List<String> topicChunks = formatter.toDiscordRawContent(
                        statusMessage.formatToComponents(config.getDiscordConsoleTopicFormat().orElseThrow()).stream(),
                        DiscordService.DISCORD_TOPIC_CHAR_LIMIT
                );
                if(topicChunks.size() > 0) {
                    discordService.setConsoleChannelTopic(topicChunks.get(0));
                }
            }
        }

        if(config.getDiscordAlertMsptThreshold().isPresent()) {
            long threshold = config.getDiscordAlertMsptThreshold().orElseThrow();
            if(now - lastMsptAlertTime > config.getDiscordAlertCooldown() * 1000 &&
                    lastMsptValue > threshold &&
                    statusMessage.getMspt() > threshold) {
                discordService.sendToConsoleChannel("%sMSPT is %.1f (configured alert threshold is %d)"
                        .formatted(getAlertTags(), statusMessage.getMspt(), threshold));
            }
            lastMsptValue = statusMessage.getMspt();
        }
    }

    @Override
    public String getOwnSourceId() {
        return DiscordService.DISCORD_SOURCE_ID;
    }

    private void publishToTopic(Consumer<String> topicPublisher, ServerStatusMessage message, String format) {

    }

    private String getAlertTags() {
        StringBuilder tags = new StringBuilder();

        if(config.getDiscordAlertUserIds().isPresent()) {
            for(long userId : config.getDiscordAlertUserIds().orElseThrow()) {
                tags.append("<@").append(userId).append("> ");
            }
        }
        if(config.getDiscordAlertRoleIds().isPresent()) {
            for(long roleId : config.getDiscordAlertRoleIds().orElseThrow()) {
                tags.append("<@&").append(roleId).append("> ");
            }
        }

        if(tags.isEmpty()) {
            tags.append("@everyone ");
        }

        return tags.toString();
    }
}
