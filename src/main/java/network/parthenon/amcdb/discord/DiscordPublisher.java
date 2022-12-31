package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.MessageHandler;
import network.parthenon.amcdb.messaging.message.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class DiscordPublisher implements MessageHandler {

    private final DiscordService discord;

    public DiscordPublisher() {
        this.discord = DiscordService.getInstance();
    }

    @Override
    public void handleMessage(InternalMessage message) {

        switch(message.getType()) {
            case CHAT:
                if(message.getAuthor() != null) {
                    DiscordFormatter
                            .toDiscordRawContent(message.formatToComponents(DiscordService.CHAT_MESSAGE_FORMAT).stream())
                            .forEach(discord::sendToChatChannel);
                }
                else {
                    DiscordFormatter.toDiscordRawContent(message.getComponents().stream())
                            .forEach(discord::sendToChatChannel);
                }
                break;
            case CONSOLE:
                DiscordFormatter.toDiscordRawContent(message.getComponents().stream())
                        .forEach(discord::sendToConsoleChannel);
                break;
        }
    }

    @Override
    public String getOwnSourceId() {
        return DiscordService.DISCORD_SOURCE_ID;
    }


}
