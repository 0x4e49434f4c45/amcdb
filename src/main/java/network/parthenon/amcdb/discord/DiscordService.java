package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.data.services.PlayerMappingService;
import network.parthenon.amcdb.messaging.MessageBroker;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern WEBHOOK_URL_PATTERN = Pattern.compile("^https://discord.com/api/webhooks/(?<id>\\d+)/(?<token>[a-zA-Z0-9_]+)$");

    private final DiscordConfig config;

    private final DiscordCommand discordCommand;

    private final MessageBroker broker;

    private final PlayerMappingService playerMappingService;

    private JDA jdaInstance;

    private TextChannel chatChannel;

    private BatchingSender chatSender;

    private WebhookSender chatWebhookSender;

    private long chatWebhookId;

    private TextChannel consoleChannel;

    private BatchingSender consoleSender;

    public DiscordService(MessageBroker broker, PlayerMappingService playerMappingService, DiscordConfig config) {

        this.config = config;

        this.broker = broker;
        this.playerMappingService = playerMappingService;

        this.discordCommand = new DiscordCommand(this, playerMappingService);
        CommandRegistrationCallback.EVENT.register(discordCommand::registerCommand);

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

        if(config.getDiscordChatWebhookUrl().isPresent()) {
            String webhookUrl = config.getDiscordChatWebhookUrl().orElseThrow();
            Matcher webhookUrlMatcher = WEBHOOK_URL_PATTERN.matcher(webhookUrl);
            if(!webhookUrlMatcher.find()) {
                AMCDB.LOGGER.warn("The configured webhook URL '%s' does not appear to be a valid Discord webhook URL! Webhook mode will not be enabled.".formatted(webhookUrl));
            }
            else {
                chatWebhookSender = new WebhookSender(webhookUrl);
                chatWebhookId = Long.parseLong(webhookUrlMatcher.group("id"), 10);
            }
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
        this.broker.subscribe(new RoleManager(playerMappingService, this, config));
    }

    /**
     * Returns whether the provided user ID corresponds to this bot
     * (either the bot user or, if enabled, the webhook).
     * @param userId The user ID to check.
     * @return
     */
    public boolean isSelf(long userId) {
        return this.jdaInstance.getSelfUser().getIdLong() == userId || chatWebhookId == userId;
    }

    /**
     * Sends the specified message to the Discord chat channel webhook, if it is enabled.
     * @param message   Message to send.
     */
    public void sendToChatChannel(String message) {
        queueMessage(chatSender, message);
    }

    /**
     * Sends the specified message to the Discord chat channel webhook, if it is enabled.
     * @param message   Message to send.
     * @param username  Name of the user who sent this message.
     * @param avatarUrl URL of an avatar image to display for the user who sent this message.
     */
    public void sendToChatWebhook(String message, String username, String avatarUrl) {
        if(isChatWebhookEnabled()) {
            chatWebhookSender.send(message, username, avatarUrl);
        }
    }

    /**
     * Sends the specified message to the Discord console channel, if it is enabled.
     * @param message Message to send.
     */
    public void sendToConsoleChannel(String message) {
        queueMessage(consoleSender, message);
    }

    /**
     * Sends a direct message to the specified User.
     * @param user
     * @param message
     * @return
     */
    public CompletableFuture<Message> sendDirectMessage(User user, String message) {
        return user.openPrivateChannel().submit()
                .thenCompose(c -> c.sendMessage(message).submit());
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

    /**
     * Gets the Member representing the specified user.
     * @param id
     * @return
     */
    public CompletableFuture<Member> retrieveChatMemberById(String id) {
        return getGuild().retrieveMemberById(id).submit();
    }

    /**
     * Gets the Member representing the specified Discord user.
     * @param username
     * @param discriminator
     * @return
     */
    public CompletableFuture<Member> findChatMemberByUsernameAndDiscriminator(String username, String discriminator) {
        CompletableFuture<Member> retrieval = new CompletableFuture<>();
        getGuild().retrieveMembersByPrefix(username, 100)
                .onError(e -> {
                    AMCDB.LOGGER.error("Failed to retrieve Discord user %s#%s".formatted(username, discriminator));
                    retrieval.completeExceptionally(e);
                })
                .onSuccess(members -> {
                    retrieval.complete(members.stream()
                            .filter(m -> m.getUser().getName().equals(username) && m.getUser().getDiscriminator().equals(discriminator))
                            .findFirst()
                            .orElse(null));
                });
        return retrieval;
    }

    public Member getChatMemberFromCache(String id) {
        return getGuild().getMemberById(id);
    }

    public Role getRoleById(String id) {
        return getGuild().getRoleById(id);
    }

    public Channel getChannelById(String id) {
        return getGuild().getChannelById(Channel.class, id);
    }

    /**
     * Adds the specified role to the specified guild member.
     * @param userId
     * @param roleId
     * @return
     */
    public CompletableFuture<Void> addRoleToUser(long userId, long roleId) {
        Role role = getGuild().getRoleById(roleId);
        if(role == null) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to get role for ID %d".formatted(roleId)));
        }
        return getGuild().addRoleToMember(UserSnowflake.fromId(userId), role).submit();
    }

    /**
     * Removes the specified role from the specified guild member.
     * @param userId
     * @param roleId
     * @return
     */
    public CompletableFuture<Void> removeRoleFromUser(long userId, long roleId) {
        Role role = getGuild().getRoleById(roleId);
        if(role == null) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to get role for ID %d".formatted(roleId)));
        }
        return getGuild().removeRoleFromMember(UserSnowflake.fromId(userId), role).submit();
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
     * Gets whether the chat channel is enabled with either the regular sender (bot)
     * or chat webhook.
     * @return
     */
    public boolean isChatChannelEnabled() {
        return chatSender != null || isChatWebhookEnabled();
    }

    /**
     * Gets whether the chat channel webhook mode is enabled.
     * @return
     */
    public boolean isChatWebhookEnabled() {
        return chatWebhookSender != null;
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

    /**
     * Gets the Guild object representing the Discord server this bot is connected to.
     * Per JDA docs, this object is not permanent. Do not cache it.
     * @return Guild
     */
    private Guild getGuild() {
        return jdaInstance.getGuildById(config.getDiscordGuildId());
    }
}
