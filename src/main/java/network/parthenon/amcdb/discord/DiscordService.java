package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;

import java.util.Optional;
import java.util.concurrent.Executors;

public class DiscordService {

    /**
     * Used to identify InternalMessages originating from Discord.
     */
    public static final String DISCORD_SOURCE_ID = "Discord";

    public static final Optional<Long> CHAT_CHANNEL_ID = AMCDBConfig.getOptionalLong("amcdb.discord.channels.chat");

    public static final Optional<Long> CONSOLE_CHANNEL_ID = AMCDBConfig.getOptionalLong("amcdb.discord.channels.console");

    private static final long batchingTimeLimitMillis = AMCDBConfig.getRequiredLong("amcdb.discord.batching.timeLimit");

    private static DiscordService instance;

    private static final String BOT_TOKEN = AMCDBConfig.getRequiredProperty("amcdb.discord.bot.token");

    private JDA jdaInstance;

    private TextChannel chatChannel;

    private BatchingSender chatSender;

    private TextChannel consoleChannel;

    private BatchingSender consoleSender;

    private DiscordService() {
        // initialize JDA
        jdaInstance = JDABuilder.createDefault(BOT_TOKEN)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DiscordListener())
                .build();

        try {
            jdaInstance.awaitReady();
        } catch(InterruptedException e) {
            throw new RuntimeException("Thread interrupted before JDA instance was ready.", e);
        }

        if(CHAT_CHANNEL_ID.isPresent()) {
            long chatChannelId = CHAT_CHANNEL_ID.orElseThrow();
            chatChannel = jdaInstance.getTextChannelById(chatChannelId);
            if(chatChannel == null) {
                throw new RuntimeException("Chat channel (" + chatChannelId + ") was not found. Check that the amcdb.discord.channels.chat property is set correctly!");
            }

            chatSender = new BatchingSender(chatChannel);
            chatSender.start(batchingTimeLimitMillis);
        }

        if(CONSOLE_CHANNEL_ID.isPresent()) {
            long consoleChannelId = CONSOLE_CHANNEL_ID.orElseThrow();
            consoleChannel = jdaInstance.getTextChannelById(consoleChannelId);
            if(consoleChannel == null) {
                throw new RuntimeException("Console channel (" + consoleChannelId + ") was not found. Check that the amcdb.discord.channels.chat property is set correctly!");
            }

            consoleSender = new BatchingSender(consoleChannel);
            consoleSender.start(batchingTimeLimitMillis);
        }
    }

    public void sendToChatChannel(String message) {
        if(chatSender == null) {
            return;
        }
        chatSender.enqueueMessage(message);
    }

    public void sendToConsoleChannel(String message) {
        if(consoleSender == null) {
            return;
        }
        consoleSender.enqueueMessage(message);
    }

    public static DiscordService getInstance() {
        return instance == null ?
                instance = new DiscordService() :
                instance;
    }

    public static void init() {

        // subscribe to internal messages (i.e. coming from Minecraft)
        BackgroundMessageBroker.subscribe(new DiscordPublisher());
    }
}
