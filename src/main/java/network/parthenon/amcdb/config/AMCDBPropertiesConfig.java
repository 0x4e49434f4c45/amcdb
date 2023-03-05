package network.parthenon.amcdb.config;

import network.parthenon.amcdb.AMCDB;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;

public class AMCDBPropertiesConfig extends ConfigBase implements AMCDBConfig, DiscordConfig, MinecraftConfig {

    /**
     * Properties file path.
     */
    private final Path propertiesPath;

    private final OptionalLong shutdownDelay;

    private final String databaseConnectionString;

    private final String databaseUsername;

    private final String databasePassword;

    private final String discordBotToken;

    private final long discordGuildId;

    private final OptionalLong discordChatChannel;

    private final Optional<String> discordChatTopicFormat;

    private final Optional<String> discordChatWebhookUrl;

    private final OptionalLong discordConsoleChannel;

    private final Optional<String> discordConsoleTopicFormat;

    private final boolean discordConsoleExecutionEnabled;

    private final boolean discordUseServerNicknames;

    private final String discordChatMessageFormat;

    private final String discordWebhookChatMessageFormat;

    private final String discordBroadcastMessageFormat;

    private final OptionalLong discordInMinecraftServerRole;

    private final long discordBatchingTimeLimit;

    private final long discordTopicUpdateInterval;
    
    private final boolean minecraftTextColorsEnabled;

    private final String minecraftMessageFormat;

    private final String minecraftAvatarApiUrl;

    private final String minecraftLogFile;

    /**
     * Creates a new AMCDBPropertiesConfig instance for the specified file.
     * @param propsPath The properties file to load.
     */
    public AMCDBPropertiesConfig(Path propsPath) {
        super(new Properties());
        propertiesPath = propsPath;

        loadConfig();

        // initialize all the config variables
        // do this now so that we can fail immediately if the config file is wrong
        shutdownDelay = getOptionalLong("amcdb.shutdown.delay");
        databaseConnectionString = getRequiredProperty("amcdb.database.connectionString");
        databaseUsername = getRequiredProperty("amcdb.database.username");
        databasePassword = getRequiredProperty("amcdb.database.password");
        discordBotToken = getRequiredProperty("amcdb.discord.bot.token");
        discordGuildId = getRequiredLong("amcdb.discord.server");
        discordChatChannel = getOptionalLong("amcdb.discord.channels.chat");
        discordChatTopicFormat = getOptionalProperty("amcdb.discord.channels.chat.topicFormat");
        discordChatWebhookUrl = getOptionalProperty("amcdb.discord.channels.chat.webhookUrl");
        discordConsoleChannel = getOptionalLong("amcdb.discord.channels.console");
        discordConsoleTopicFormat = getOptionalProperty("amcdb.discord.channels.console.topicFormat");
        discordConsoleExecutionEnabled = getOptionalBoolean("amcdb.discord.channels.console.enableExecution", false);
        discordUseServerNicknames = getOptionalBoolean("amcdb.discord.useServerNicknames", true);
        discordBroadcastMessageFormat = getRequiredProperty("amcdb.discord.broadcastMessageFormat");
        discordChatMessageFormat = getRequiredProperty("amcdb.discord.chatMessageFormat");
        discordWebhookChatMessageFormat = getRequiredProperty("amcdb.discord.webhookChatMessageFormat");
        discordInMinecraftServerRole = getOptionalLong("amcdb.discord.role.inMinecraftServer");
        discordTopicUpdateInterval = getRequiredLong("amcdb.discord.topicUpdateInterval");
        discordBatchingTimeLimit = getRequiredLong("amcdb.discord.batching.timeLimit");
        minecraftLogFile = getRequiredProperty("amcdb.minecraft.logFile");
        minecraftMessageFormat = getRequiredProperty("amcdb.minecraft.messageFormat");
        minecraftTextColorsEnabled = getOptionalBoolean("amcdb.minecraft.showTextColors", true);
        minecraftAvatarApiUrl = getRequiredProperty("amcdb.minecraft.avatarApi.url");
    }

    private void loadConfig() {
        if(Files.notExists(propertiesPath)) {
            // copy from resources
            try(InputStream rsProperties = AMCDBPropertiesConfig.class.getClassLoader().getResourceAsStream("amcdb.properties")) {
                Files.copy(rsProperties, propertiesPath);
            } catch (IOException e) {
                AMCDB.LOGGER.error("Failed to write AMCDB properties. Make sure that the server is able to write to the config directory.");
                throw new RuntimeException("Failed to write AMCDB properties file", e);
            }
        }

        try(InputStream propertiesStream = Files.newInputStream(propertiesPath)) {
            properties.load(propertiesStream);
        } catch (IOException e) {
            AMCDB.LOGGER.error("Failed to load AMCDB properties. Make sure that the server is able to read the file config/amcdb.properties.");
            throw new RuntimeException("Failed to load AMCDB properties file", e);
        }
    }

    // Getters below


    @Override
    public OptionalLong getShutdownDelay() {
        return shutdownDelay;
    }

    @Override
    public String getDatabaseConnectionString() {
        return databaseConnectionString;
    }

    @Override
    public String getDatabaseUsername() { return databaseUsername; }

    @Override
    public String getDatabasePassword() { return databasePassword; }

    @Override
    public String getDiscordBotToken() {
        return discordBotToken;
    }

    @Override
    public long getDiscordGuildId() { return discordGuildId; }

    @Override
    public OptionalLong getDiscordChatChannel() {
        return discordChatChannel;
    }

    @Override
    public Optional<String> getDiscordChatTopicFormat() {
        return discordChatTopicFormat;
    }

    @Override
    public Optional<String> getDiscordChatWebhookUrl() { return discordChatWebhookUrl; }

    @Override
    public OptionalLong getDiscordConsoleChannel() {
        return discordConsoleChannel;
    }

    @Override
    public Optional<String> getDiscordConsoleTopicFormat() {
        return discordConsoleTopicFormat;
    }

    @Override
    public boolean getDiscordConsoleExecutionEnabled() {
        return discordConsoleExecutionEnabled;
    }

    @Override
    public boolean getDiscordUseServerNicknames() {
        return discordUseServerNicknames;
    }

    @Override
    public String getDiscordChatMessageFormat() {
        return discordChatMessageFormat;
    }

    @Override
    public String getDiscordWebhookChatMessageFormat() { return discordWebhookChatMessageFormat; }

    @Override
    public String getDiscordBroadcastMessageFormat() {
        return discordBroadcastMessageFormat;
    }

    @Override
    public OptionalLong getDiscordInMinecraftServerRole() { return discordInMinecraftServerRole; }

    @Override
    public long getDiscordBatchingTimeLimit() {
        return discordBatchingTimeLimit;
    }

    @Override
    public long getDiscordTopicUpdateInterval() {
        return discordTopicUpdateInterval;
    }

    @Override
    public boolean getMinecraftTextColorsEnabled() {
        return minecraftTextColorsEnabled;
    }

    @Override
    public String getMinecraftMessageFormat() {
        return minecraftMessageFormat;
    }

    @Override
    public String getMinecraftAvatarApiUrl() { return minecraftAvatarApiUrl; }

    @Override
    public String getMinecraftLogFile() {
        return minecraftLogFile;
    }
}
