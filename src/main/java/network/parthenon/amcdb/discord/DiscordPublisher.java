package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.message.*;
import network.parthenon.amcdb.messaging.MessageHandler;

import java.util.Date;
import java.util.List;

public class DiscordPublisher implements MessageHandler {

    private final DiscordService discord;

    public DiscordPublisher() {
        this.discord = DiscordService.getInstance();
    }

    private long lastTopicUpdateTime = 0;

    @Override
    public void handleMessage(InternalMessage message) {

        if(message instanceof ChatMessage && DiscordService.getInstance().isChatChannelEnabled()) {
            DiscordFormatter
                    .toDiscordRawContent(
                            message.formatToComponents(DiscordService.CHAT_MESSAGE_FORMAT).stream(),
                            DiscordService.DISCORD_MESSAGE_CHAR_LIMIT)
                    .forEach(discord::sendToChatChannel);
        }
        else if(message instanceof BroadcastMessage && DiscordService.getInstance().isChatChannelEnabled()) {
            DiscordFormatter
                    .toDiscordRawContent(
                            message.formatToComponents(DiscordService.BROADCAST_MESSAGE_FORMAT).stream(),
                            DiscordService.DISCORD_MESSAGE_CHAR_LIMIT)
                    .forEach(discord::sendToChatChannel);
        }
        else if(message instanceof ConsoleMessage && DiscordService.getInstance().isConsoleChannelEnabled()) {
            DiscordFormatter
                    .toDiscordRawContent(
                            message.getComponents().stream(),
                            DiscordService.DISCORD_MESSAGE_CHAR_LIMIT)
                    .forEach(discord::sendToConsoleChannel);
        }
        else if(message instanceof ServerStatusMessage) {
            publishChannelTopics((ServerStatusMessage) message);
        }
    }

    @Override
    public String getOwnSourceId() {
        return DiscordService.DISCORD_SOURCE_ID;
    }

    /**
     * Updates the Discord channel topics, if this feature is enabled.
     * @param message Server status information with which to update channel topics.
     */
    private void publishChannelTopics(ServerStatusMessage message) {
        AMCDB.LOGGER.debug(message.toString());

        long now = System.currentTimeMillis();

        if(now - lastTopicUpdateTime > DiscordService.TOPIC_UPDATE_INTERVAL_SECONDS * 1000) {
            lastTopicUpdateTime = now;

            if(DiscordService.CHAT_TOPIC_FORMAT.isPresent()) {
                List<String> topicChunks = DiscordFormatter.toDiscordRawContent(
                        message.formatToComponents(DiscordService.CHAT_TOPIC_FORMAT.get()).stream(),
                        DiscordService.DISCORD_TOPIC_CHAR_LIMIT
                );
                if(topicChunks.size() > 0) {
                    DiscordService.getInstance().setChatChannelTopic(topicChunks.get(0));
                    AMCDB.LOGGER.debug("Attempted to update chat channel topic");
                }
            }

            if(DiscordService.CONSOLE_TOPIC_FORMAT.isPresent()) {
                List<String> topicChunks = DiscordFormatter.toDiscordRawContent(
                        message.formatToComponents(DiscordService.CONSOLE_TOPIC_FORMAT.get()).stream(),
                        DiscordService.DISCORD_TOPIC_CHAR_LIMIT
                );
                if(topicChunks.size() > 0) {
                    DiscordService.getInstance().setConsoleChannelTopic(topicChunks.get(0));
                    AMCDB.LOGGER.debug("Attempted to update console channel topic");
                }
            }
        }
    }

}
