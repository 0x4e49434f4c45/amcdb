package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;
import network.parthenon.amcdb.messaging.component.EntityReference;

import java.util.List;

public class DiscordListener extends ListenerAdapter {

    private final DiscordService discordService;

    private final DiscordConfig config;

    private final DiscordFormatter formatter;

    private final BackgroundMessageBroker broker;

    public DiscordListener(DiscordService discordService, DiscordConfig config, BackgroundMessageBroker broker) {
        this.discordService = discordService;
        this.config = config;
        this.formatter = new DiscordFormatter(discordService, config);
        this.broker = broker;
    }

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

        if(config.getDiscordChatChannel().isPresent()
                && e.getChannel().getIdLong() == config.getDiscordChatChannel().orElseThrow()) {
            handleChatMessage(e.getMessage());
        }
        else if(config.getDiscordChatChannel().isPresent()
                && e.getChannel().getIdLong() == config.getDiscordChatChannel().orElseThrow()) {
            handleConsoleMessage(e.getMessage());
        }
    }

    /**
     * Publishes a chat message to the internal message broker.
     *
     * @param message The Discord message to publish.
     */
    private void handleChatMessage(Message message) {
        // if this is a reply, add a snippet of the original
        Message referencedMessage = message.getReferencedMessage();
        BroadcastMessage replySnippetMessage = null;
        if(referencedMessage != null) {
            // create a temporary ChatMessage to easily format the referenced message
            // consistently with other messages
            List<InternalMessageComponent> referencedComponents = new ChatMessage(
                    DiscordService.DISCORD_SOURCE_ID,
                    formatter.getAuthorReference(referencedMessage, true),
                    formatter.toComponents(referencedMessage.getContentRaw(), referencedMessage.getAttachments())
            // thanks to Xujiayao (author of https://github.com/Xujiayao/MCDiscordChat) for this bit of Unicode
            ).formatToComponents("┌───%username% %message%", 50, new TextComponent("..."));
            replySnippetMessage = new BroadcastMessage(DiscordService.DISCORD_SOURCE_ID, referencedComponents);
        }

        InternalMessage internalMessage = new ChatMessage(
                DiscordService.DISCORD_SOURCE_ID,
                formatter.getAuthorReference(message, false),
                formatter.toComponents(message.getContentRaw(), message.getAttachments())
        );

        if(replySnippetMessage != null) {
            broker.publish(replySnippetMessage, internalMessage);
        }
        else {
            broker.publish(internalMessage);
        }
    }

    /**
     * Publishes a console channel message (i.e. console command) to the internal message broker.
     *
     * @param message The Discord message to publish.
     */
    private void handleConsoleMessage(Message message) {
        if(!config.getDiscordConsoleExecutionEnabled()) {
            discordService.sendToConsoleChannel("%s, command execution via console is not enabled. Set `amcdb.discord.channels.console.enableExecution=true` in the configuration file to enable this feature.".formatted(message.getAuthor().getAsMention()));
            return;
        }

        InternalMessage internalMessage = new ConsoleMessage(
                DiscordService.DISCORD_SOURCE_ID,
                formatter.getAuthorReference(message, false),
                formatter.toComponents(message.getContentRaw())
        );

        broker.publish(internalMessage);
    }
}
