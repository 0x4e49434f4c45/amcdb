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

    private long lastTopicUpdateTime = 0;

    private long lastMsptAlertTime = 0;

    public DiscordPublisher(DiscordService discordService, DiscordConfig config) {
        this.discordService = discordService;
        this.config = config;
        this.formatter = new DiscordFormatter(discordService, config);
    }

    @Override
    public void handleMessage(InternalMessage message) {

        if(message instanceof ChatMessage && discordService.isChatChannelEnabled() && !isFiltered(message)) {
            if(config.getDiscordIgnoredExternalUsers().isPresent() &&
                config.getDiscordIgnoredExternalUsers().orElseThrow().contains(((ChatMessage) message).getAuthor().getAlternateName())) {
                return;
            }
            String messageFormat = discordService.isChatWebhookEnabled() ?
                    config.getDiscordWebhookChatMessageFormat() :
                    config.getDiscordChatMessageFormat();
            List<String> messageParts = formatter.toDiscordRawContent(
                    message.formatToComponents(messageFormat).stream(),
                    DiscordService.DISCORD_MESSAGE_CHAR_LIMIT);

            EntityReference author = ((ChatMessage) message).getAuthor();

            sendChatMessage(messageParts, author.getDisplayName(), author.getImageUrl());
        }
        else if(message instanceof BroadcastMessage && !config.getDiscordIgnoreBroadcast() && discordService.isChatChannelEnabled() && !isFiltered(message)) {
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
        else if(message instanceof ServerLifecycleMessage && discordService.isChatChannelEnabled()) {
            publishLifecycleMessage((ServerLifecycleMessage) message);
        }
    }

    /**
     * Publishes a server lifecycle message according to the applicable configuration.
     */
    private void publishLifecycleMessage(ServerLifecycleMessage message) {
        String format = null;

        if(message.getEvent() == ServerLifecycleMessage.Event.STARTED && config.getDiscordLifecycleStartedFormat().isPresent()) {
            format = config.getDiscordLifecycleStartedFormat().orElseThrow();
        }
        else if(message.getEvent() == ServerLifecycleMessage.Event.STOPPED && config.getDiscordLifecycleStoppedFormat().isPresent()) {
            format = config.getDiscordLifecycleStoppedFormat().orElseThrow();
        }

        if(format != null) {
            sendChatMessage(
                    formatter.toDiscordRawContent(
                            message.formatToComponents(format).stream(),
                            DiscordService.DISCORD_MESSAGE_CHAR_LIMIT),
                    null,
                    null);
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

    private boolean isFiltered(InternalMessage message) {
        return config.getDiscordMessageFilterPattern().isPresent() &&
            config.getDiscordMessageFilterPattern().orElseThrow().matcher(message.getUnformattedContents()).find() ==
                    config.getDiscordMessageFilterExclude();
    }

}
