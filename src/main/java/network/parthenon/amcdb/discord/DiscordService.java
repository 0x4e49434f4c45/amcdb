package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import network.parthenon.amcdb.config.AMCDBPropertiesConfig;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DiscordService {

    /**
     * Maximum number of characters allowed in a Discord message.
     */
    public static final int DISCORD_MESSAGE_CHAR_LIMIT = 2000;

    public static final int DISCORD_TOPIC_CHAR_LIMIT = 1024;

    /**
     * Used to identify InternalMessages originating from Discord.
     */
    public static final String DISCORD_SOURCE_ID = "Discord";

    private final DiscordConfig config;

    private final BackgroundMessageBroker broker;

    private JDA jdaInstance;

    private TextChannel chatChannel;

    private BatchingSender chatSender;

    private TextChannel consoleChannel;

    private BatchingSender consoleSender;

    public DiscordService(BackgroundMessageBroker broker, DiscordConfig config) {

        this.config = config;

        this.broker = broker;

        // initialize JDA
        jdaInstance = JDABuilder.createDefault(config.getDiscordBotToken())
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DiscordListener(this, config, broker))
                .build();

        do {
            try {
                jdaInstance.awaitReady();
            } catch (InterruptedException e) { }
        } while (jdaInstance.getStatus() != JDA.Status.CONNECTED);

        if(config.getDiscordChatChannel().isPresent()) {
            long chatChannelId = config.getDiscordChatChannel().orElseThrow();
            chatChannel = jdaInstance.getTextChannelById(chatChannelId);
            if(chatChannel == null) {
                throw new RuntimeException("Chat channel (" + chatChannelId + ") was not found. Check that the amcdb.discord.channels.chat property is set correctly!");
            }

            chatSender = new BatchingSender(chatChannel);
            chatSender.start(config.getDiscordBatchingTimeLimit());
        }

        if(config.getDiscordConsoleChannel().isPresent()) {
            long consoleChannelId = config.getDiscordConsoleChannel().orElseThrow();
            consoleChannel = jdaInstance.getTextChannelById(consoleChannelId);
            if(consoleChannel == null) {
                throw new RuntimeException("Console channel (" + consoleChannelId + ") was not found. Check that the amcdb.discord.channels.chat property is set correctly!");
            }

            consoleSender = new BatchingSender(consoleChannel);
            consoleSender.start(config.getDiscordBatchingTimeLimit());
        }

        // subscribe to internal messages (i.e. coming from Minecraft)
        this.broker.subscribe(new DiscordPublisher(this, config));
    }

    /**
     * Sends the specified message to the Discord chat channel, if it is enabled.
     * @param message Message to send.
     */
    public void sendToChatChannel(String message) {
        queueMessage(chatSender, message);
    }

    /**
     * Sends the specified message to the Discord console channel, if it is enabled.
     * @param message Message to send.
     */
    public void sendToConsoleChannel(String message) {
        queueMessage(consoleSender, message);
    }

    /**
     * Sends the specified message using the specified sender, if it is not null.
     * @param sender  The sender to use. If null, this method does nothing.
     * @param message Message to send. It is an error to supply a message longer than {@link #DISCORD_MESSAGE_CHAR_LIMIT}.
     */
    private void queueMessage(BatchingSender sender, String message) {
        if(message.length() > DISCORD_MESSAGE_CHAR_LIMIT) {
            throw new IllegalArgumentException("Message is too long for Discord! (length: %d)".formatted(message.length()));
        }
        if(sender == null) {
            return;
        }
        sender.enqueueMessage(message);
    }

    public CompletableFuture<Member> retrieveChatMemberById(String id) {
        return chatChannel.getGuild().retrieveMemberById(id).submit()
                .whenComplete((v, error) -> {});
    }

    public Member getChatMemberFromCache(String id) {
        return chatChannel.getGuild().getMemberById(id);
    }

    public Role getRoleById(String id) {
        return chatChannel.getGuild().getRoleById(id);
    }

    public Channel getChannelById(String id) {
        return chatChannel.getGuild().getChannelById(Channel.class, id);
    }

    /**
     * Sets the chat channel topic to the provided string.
     * @param topic The topic to set.
     */
    public void setChatChannelTopic(String topic) {
        setChannelTopic(chatChannel, topic);
    }

    /**
     * Sets the console channel topic to the provided string.
     * @param topic The topic to set.
     */
    public void setConsoleChannelTopic(String topic) {
        setChannelTopic(consoleChannel, topic);
    }

    /**
     * Sets the specified channel topic to the provided string,
     * if the channel is not null.
     * @param channel The channel on which to set the topic.
     * @param topic   The topic to set.
     */
    private void setChannelTopic(TextChannel channel, String topic) {
        if(channel != null) {
            // disable JDA queueing on this request.
            // if JDA gets a 429 rate limit error with queuing enabled, it automatically
            // tries to resend the request after the rate limit expires, which is
            // counterproductive because the data is old at that point and the extra
            // request causes the next intentional topic update to get rate limited.
            channel.getManager().setTopic(topic).submit(false);
        }
    }

    /**
     * Gets whether the chat channel is enabled.
     * @return
     */
    public boolean isChatChannelEnabled() {
        return chatSender != null;
    }

    /**
     * Gets whether the console channel is enabled.
     * @return
     */
    public boolean isConsoleChannelEnabled() {
        return consoleSender != null;
    }

    /**
     * Shuts down the internal JDA instance.
     */
    public void shutdown() {
        jdaInstance.shutdown();
    }
}
