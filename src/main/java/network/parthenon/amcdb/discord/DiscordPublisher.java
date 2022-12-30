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
        List<String> discordMessages = DiscordFormatter.toDiscordRawContent(message);

        switch(message.getType()) {
            case CHAT:
                discordMessages.forEach(discord::sendToChatChannel);
                break;
            case CONSOLE:
                discordMessages.forEach(discord::sendToConsoleChannel);
                break;
        }
    }

    @Override
    public String getOwnSourceId() {
        return DiscordService.DISCORD_SOURCE_ID;
    }


}
