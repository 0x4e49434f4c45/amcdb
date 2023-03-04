package network.parthenon.amcdb.config;

import network.parthenon.amcdb.AMCDB;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;

public class AMCDBPropertiesConfig implements AMCDBConfig, DiscordConfig, MinecraftConfig {

    /**
     * Properties file path.
     */
    private final Path propertiesPath;

    /**
     * Properties object to hold properties loaded from file.
     */
    private final Properties properties = new Properties();

    private final OptionalLong shutdownDelay;

    private final String databaseConnectionString;

    private final String discordBotToken;

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
        propertiesPath = propsPath;

        loadConfig();

        // initialize all the config variables
        // do this now so that we can fail immediately if the config file is wrong
        shutdownDelay = getOptionalLong("amcdb.shutdown.delay");
        databaseConnectionString = getRequiredProperty("amcdb.database.connectionString");
        discordBotToken = getRequiredProperty("amcdb.discord.bot.token");
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

    public String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if(value == null) {
            throw new RuntimeException("The required property " + key + " was not found in amcdb.properties!");
        }
        return value;
    }

    public long getRequiredLong(String key) {
        try {
            return Long.parseLong(getRequiredProperty(key), 10);
        } catch (NumberFormatException e) {
            throw new RuntimeException("The property " + key + " must be a number!");
        }
    }

    public boolean getRequiredBoolean(String key) {
        try {
            return parseBoolean(getRequiredProperty(key));
        }
        catch(RuntimeException e) {
            throw new RuntimeException("The property " + key + " must be 'true' or 'false'!");
        }
    }

    public Optional<String> getOptionalProperty(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    public OptionalLong getOptionalLong(String key) {
        String value = properties.getProperty(key);
        if(value == null) {
            return OptionalLong.empty();
        }

        try {
            return OptionalLong.of(Long.parseLong(value, 10));
        } catch (NumberFormatException e) {
            throw new RuntimeException("When the property " + key + " is set, it must be a number!");
        }
    }

    public long getOptionalLong(String key, long defaultValue) {
        OptionalLong opt = getOptionalLong(key);
        return opt.isPresent() ? opt.orElseThrow() : defaultValue;
    }

    public Optional<Boolean> getOptionalBoolean(String key) {
        String value = properties.getProperty(key);
        if(value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(parseBoolean(value));
        }
        catch(RuntimeException e) {
            throw new RuntimeException("When the property " + key + " is set, it must be 'true' or 'false'!");
        }
    }

    public boolean getOptionalBoolean(String key, boolean defaultValue) {
        Optional<Boolean> opt = getOptionalBoolean(key);
        return opt.isPresent() ? opt.orElseThrow() : defaultValue;
    }

    public String getPropertyOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    private boolean parseBoolean(String value) {
        if(value.equalsIgnoreCase("true")) {
            return true;
        }
        else if(value.equalsIgnoreCase("false")) {
            return false;
        }

        throw new RuntimeException("Invalid boolean value");
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
    public String getDiscordBotToken() {
        return discordBotToken;
    }

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
