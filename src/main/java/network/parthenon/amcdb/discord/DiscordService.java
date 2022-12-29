package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.messaging.ThreadPoolMessageBroker;

public class DiscordService {

    /**
     * Used to identify InternalMessages originating from Discord.
     */
    public static final String DISCORD_SOURCE_ID = "Discord";

    public static final long CHAT_CHANNEL_ID = AMCDBConfig.getRequiredLong("amcdb.discord.channels.chat");

    private static DiscordService instance;

    private static final String BOT_TOKEN = AMCDBConfig.getRequiredProperty("amcdb.discord.bot.token");

    private JDA jdaInstance;

    private TextChannel chatChannel;

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

        chatChannel = jdaInstance.getTextChannelById(CHAT_CHANNEL_ID);
        if(chatChannel == null) {
            throw new RuntimeException("Chat channel (" + CHAT_CHANNEL_ID + ") was not found. Check that the amcdb.discord.channels.chat property is set correctly!");
        }
    }

    public void sendToChatChannel(String message) {
        sendDiscordMessage(chatChannel, message);
    }

    /**
     * Sends a message to the specified channel, 2,000 characters at a time.
     * @param channel The channel to send to.
     * @param message The message to send.
     */
    private void sendDiscordMessage(TextChannel channel, String message) {
        int index = 0;

        while(message.length() - index > 0) {
            channel.sendMessage(message.substring(index, Math.min(message.length(), index + 2000))).queue();
            index += 2000;
        }
    }

    public static DiscordService getInstance() {
        return instance == null ?
                instance = new DiscordService() :
                instance;
    }

    public static void init() {

        // subscribe to internal messages (i.e. coming from Minecraft)
        ThreadPoolMessageBroker.subscribe(new DiscordPublisher());
    }
}
