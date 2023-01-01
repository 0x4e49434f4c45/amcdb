package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.MessageHandler;

public class DiscordPublisher implements MessageHandler {

    private final DiscordService discord;

    public DiscordPublisher() {
        this.discord = DiscordService.getInstance();
    }

    @Override
    public void handleMessage(InternalMessage message) {

        if(message instanceof ChatMessage && DiscordService.getInstance().isChatChannelEnabled()) {
            DiscordFormatter
                    .toDiscordRawContent(message.formatToComponents(DiscordService.CHAT_MESSAGE_FORMAT).stream())
                    .forEach(discord::sendToChatChannel);
        }
        else if(message instanceof BroadcastMessage && DiscordService.getInstance().isChatChannelEnabled()) {
            DiscordFormatter
                    .toDiscordRawContent(message.formatToComponents(DiscordService.BROADCAST_MESSAGE_FORMAT).stream())
                    .forEach(discord::sendToChatChannel);
        }
        else if(message instanceof ConsoleMessage && DiscordService.getInstance().isConsoleChannelEnabled()) {
            DiscordFormatter.toDiscordRawContent(message.getComponents().stream())
                    .forEach(discord::sendToConsoleChannel);
        }
    }

    @Override
    public String getOwnSourceId() {
        return DiscordService.DISCORD_SOURCE_ID;
    }

}
