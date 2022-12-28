package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.messaging.InternalMessage;
import network.parthenon.amcdb.messaging.MessageHandler;

public class DiscordPublisher implements MessageHandler {

    private final DiscordService discord;

    public DiscordPublisher() {
        this.discord = DiscordService.getInstance();
    }

    @Override
    public void handleMessage(InternalMessage message) {
        String discordMessage;

        // TODO: discord formatter
        if(message.getAuthor() != null) {
            discordMessage = "<" + message.getAuthor().getDisplayName() + "> " + message.toString();
        }
        else {
            discordMessage = message.toString();
        }
        discord.sendToChatChannel(discordMessage);
    }
}
