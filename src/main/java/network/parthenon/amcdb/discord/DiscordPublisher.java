package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.*;
import network.parthenon.amcdb.messaging.MessageHandler;

import java.util.List;
import java.util.stream.Stream;

public class DiscordPublisher implements MessageHandler {

    private final DiscordService discordService;

    private final DiscordConfig config;

    private final DiscordFormatter formatter;

    public DiscordPublisher(DiscordService discordService, DiscordConfig config) {
        this.discordService = discordService;
        this.config = config;
        this.formatter = new DiscordFormatter(discordService, config);
    }

    private long lastTopicUpdateTime = 0;

    @Override
    public void handleMessage(InternalMessage message) {

        if(message instanceof ChatMessage && discordService.isChatChannelEnabled()) {
            String messageFormat = discordService.isChatWebhookEnabled() ?
                    config.getDiscordWebhookChatMessageFormat() :
                    config.getDiscordChatMessageFormat();
            List<String> messageParts = formatter.toDiscordRawContent(
                    message.formatToComponents(messageFormat).stream(),
                    DiscordService.DISCORD_MESSAGE_CHAR_LIMIT);

            EntityReference author = ((ChatMessage) message).getAuthor();

            sendChatMessage(messageParts, author.getDisplayName(), author.getImageUrl());
        }
        else if(message instanceof BroadcastMessage && discordService.isChatChannelEnabled()) {
            List<String> messageParts = formatter.toDiscordRawContent(
                            message.formatToComponents(config.getDiscordBroadcastMessageFormat()).stream(),
                            DiscordService.DISCORD_MESSAGE_CHAR_LIMIT);

            sendChatMessage(messageParts, null, null);
        }
        else if(message instanceof ConsoleMessage && discordService.isConsoleChannelEnabled()) {
            formatter.toDiscordRawContent(
                            message.getComponents().stream(),
                            DiscordService.DISCORD_MESSAGE_CHAR_LIMIT)
                    .forEach(discordService::sendToConsoleChannel);
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
     * Sends a message to the chat channel using the webhook if enabled or the bot otherwise.
     * @param messageParts The message part(s) to send.
     * @param username     The username of the sender (displayed only in webhook mode).
     *                     Null for the webhook default name.
     * @param avatarUrl    URL of an avatar image to display for the user who sent this message.
     *                     Used only in webhook mode. Null for the webhook default avatar.
     */
    private void sendChatMessage(List<String> messageParts, String username, String avatarUrl) {
        if(discordService.isChatWebhookEnabled()) {
            messageParts.forEach(m -> discordService.sendToChatWebhook(m, username, avatarUrl));
        }
        else {
            messageParts.forEach(discordService::sendToChatChannel);
        }
    }

    /**
     * Updates the Discord channel topics, if this feature is enabled.
     * @param message Server status information with which to update channel topics.
     */
    private void publishChannelTopics(ServerStatusMessage message) {
        AMCDB.LOGGER.debug(message.toString());

        long now = System.currentTimeMillis();

        if(now - lastTopicUpdateTime > config.getDiscordTopicUpdateInterval() * 1000) {
            lastTopicUpdateTime = now;

            if(config.getDiscordChatTopicFormat().isPresent()) {
                List<String> topicChunks = formatter.toDiscordRawContent(
                        message.formatToComponents(config.getDiscordChatTopicFormat().get()).stream(),
                        DiscordService.DISCORD_TOPIC_CHAR_LIMIT
                );
                if(topicChunks.size() > 0) {
                    discordService.setChatChannelTopic(topicChunks.get(0));
                    AMCDB.LOGGER.debug("Attempted to update chat channel topic");
                }
            }

            if(config.getDiscordConsoleTopicFormat().isPresent()) {
                List<String> topicChunks = formatter.toDiscordRawContent(
                        message.formatToComponents(config.getDiscordConsoleTopicFormat().get()).stream(),
                        DiscordService.DISCORD_TOPIC_CHAR_LIMIT
                );
                if(topicChunks.size() > 0) {
                    discordService.setConsoleChannelTopic(topicChunks.get(0));
                    AMCDB.LOGGER.debug("Attempted to update console channel topic");
                }
            }
        }
    }

}
