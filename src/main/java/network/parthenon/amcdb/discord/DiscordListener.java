package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import network.parthenon.amcdb.messaging.InternalMessage;
import network.parthenon.amcdb.messaging.ThreadPoolMessageBroker;
import network.parthenon.amcdb.messaging.UserReference;

public class DiscordListener extends ListenerAdapter {

    /**
     * Handles the JDA message received event.
     * @param e The event.
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        // Ignore messages from self or Discord system
        if(e.getJDA().getSelfUser().getIdLong() == e.getAuthor().getIdLong()
                || e.getAuthor().isSystem()
        ) {
            return;
        }

        if(e.getChannel().getIdLong() == DiscordService.CHAT_CHANNEL_ID) {
            handleChatMessage(e.getMessage());
        }
    }

    /**
     * Publishes a chat message to the internal message broker.
     *
     * @param message The Discord message to publish.
     */
    private void handleChatMessage(Message message) {
        InternalMessage internalMessage = new InternalMessage(
                DiscordService.DISCORD_SOURCE_ID,
                // TODO: handle user's role color
                new UserReference(message.getAuthor().getId(), message.getAuthor().getName()),
                DiscordFormatter.toComponents(message.getContentRaw())
        );

        ThreadPoolMessageBroker.publish(internalMessage);
    }
}
