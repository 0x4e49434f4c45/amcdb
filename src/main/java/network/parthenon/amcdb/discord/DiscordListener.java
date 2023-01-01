package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;
import network.parthenon.amcdb.messaging.component.EntityReference;

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

        if(DiscordService.CHAT_CHANNEL_ID.isPresent()
                && e.getChannel().getIdLong() == DiscordService.CHAT_CHANNEL_ID.orElseThrow()) {
            handleChatMessage(e.getMessage());
        }
        else if(DiscordService.CONSOLE_CHANNEL_ID.isPresent()
                && e.getChannel().getIdLong() == DiscordService.CONSOLE_CHANNEL_ID.orElseThrow()) {
            handleConsoleMessage(e.getMessage());
        }
    }

    /**
     * Publishes a chat message to the internal message broker.
     *
     * @param message The Discord message to publish.
     */
    private void handleChatMessage(Message message) {
        InternalMessage internalMessage = new ChatMessage(
                DiscordService.DISCORD_SOURCE_ID,
                new EntityReference(message.getMember().getId(), DiscordFormatter.getDisplayName(message.getMember()), message.getMember().getColor()),
                DiscordFormatter.toComponents(message.getContentRaw())
        );

        BackgroundMessageBroker.publish(internalMessage);
    }

    /**
     * Publishes a console channel message (i.e. console command) to the internal message broker.
     *
     * @param message The Discord message to publish.
     */
    private void handleConsoleMessage(Message message) {
        if(!DiscordService.ENABLE_CONSOLE_EXECUTION) {
            DiscordService.getInstance().sendToConsoleChannel("%s, command execution via console is not enabled. Set `amcdb.discord.channels.console.enableExecution=true` in the configuration file to enable this feature.".formatted(message.getAuthor().getAsMention()));
            return;
        }

        InternalMessage internalMessage = new ConsoleMessage(
                DiscordService.DISCORD_SOURCE_ID,
                DiscordFormatter.toComponents(message.getContentRaw())
        );

        BackgroundMessageBroker.publish(internalMessage);
    }
}
