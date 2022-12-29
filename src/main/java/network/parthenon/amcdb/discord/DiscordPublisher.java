package network.parthenon.amcdb.discord;

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
            discordMessage = "<" + DiscordFormatter.escapeMarkdown(message.getAuthor().getDisplayName()) + "> "
                    + DiscordFormatter.toDiscordRawContent(message.getComponents());
        }
        else {
            discordMessage = message.toString();
        }
        discord.sendToChatChannel(discordMessage);
    }

    @Override
    public String getOwnSourceId() {
        return DiscordService.DISCORD_SOURCE_ID;
    }


}
