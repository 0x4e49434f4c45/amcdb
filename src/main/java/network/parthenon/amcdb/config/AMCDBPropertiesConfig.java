package network.parthenon.amcdb.config;

import network.parthenon.amcdb.AMCDB;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

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

    private final Optional<Pattern> discordMessageFilterPattern;

    private final boolean discordMessageFilterExclude;

    private final Optional<List<String>> discordIgnoredExternalUsers;

    private final boolean discordIgnoreBroadcast;

    private final Optional<String> discordLifecycleStartedFormat;

    private final Optional<String> discordLifecycleStoppedFormat;

    private final OptionalLong discordAlertMsptThreshold;

    private final Optional<List<Long>> discordAlertUserIds;

    private final Optional<List<Long>> discordAlertRoleIds;

    private final long discordAlertCooldown;

    private final long discordBatchingTimeLimit;

    private final long discordTopicUpdateInterval;
    
    private final boolean minecraftTextColorsEnabled;

    private final String minecraftMessageFormat;

    private final Optional<Pattern> minecraftMessageFilterPattern;

    private final boolean minecraftMessageFilterExclude;

    private final Optional<List<String>> minecraftIgnoredExternalUsers;

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
        discordMessageFilterPattern = getOptionalRegex("amcdb.discord.messageFilter.pattern");
        discordMessageFilterExclude = getOptionalBoolean("amcdb.discord.messageFilter.exclude", true);
        discordIgnoredExternalUsers = getOptionalList("amcdb.discord.ignoredExternalUsers");
        discordIgnoreBroadcast = getOptionalBoolean("amcdb.discord.ignoreBroadcast", false);
        discordLifecycleStartedFormat = getOptionalProperty("amcdb.discord.lifecycle.startedFormat");
        discordLifecycleStoppedFormat = getOptionalProperty("amcdb.discord.lifecycle.stoppedFormat");
        discordAlertMsptThreshold = getOptionalLong("amcdb.discord.alert.msptThreshold");
        discordAlertUserIds = getOptionalLongList("amcdb.discord.alert.userIds");
        discordAlertRoleIds = getOptionalLongList("amcdb.discord.alert.roleIds");
        discordAlertCooldown = getRequiredLong("amcdb.discord.alert.cooldown");
        discordTopicUpdateInterval = getRequiredLong("amcdb.discord.topicUpdateInterval");
        discordBatchingTimeLimit = getRequiredLong("amcdb.discord.batching.timeLimit");
        minecraftLogFile = getRequiredProperty("amcdb.minecraft.logFile");
        minecraftMessageFormat = getRequiredProperty("amcdb.minecraft.messageFormat");
        minecraftMessageFilterPattern = getOptionalRegex("amcdb.minecraft.messageFilter.pattern");
        minecraftMessageFilterExclude = getOptionalBoolean("amcdb.minecraft.messageFilter.exclude", true);
        minecraftIgnoredExternalUsers = getOptionalList("amcdb.minecraft.ignoredExternalUsers");
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
        return getRequiredProperty(key, v -> v);
    }

    public long getRequiredLong(String key) {
        return getRequiredProperty(key, v -> parseLong(v, key));
    }

    public boolean getRequiredBoolean(String key) {
        return getRequiredProperty(key, v -> parseBoolean(v, key));
    }

    public List<String> getRequiredList(String key) {
        return getRequiredProperty(key, v -> Arrays.asList(v.split(",")));
    }

    public List<Long> getRequiredLongList(String key) {
        return getRequiredProperty(key, v -> Arrays.stream(v.split(",")).map(num -> parseLong(num, key)).toList());
    }

    public Pattern getRequiredRegex(String key) {
        return getRequiredProperty(key, Pattern::compile);
    }

    public Optional<String> getOptionalProperty(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    public OptionalLong getOptionalLong(String key) {
        String value = properties.getProperty(key);
        if(value == null) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(parseLong(value, key));
    }

    public long getOptionalLong(String key, long defaultValue) {
        return getOptionalLong(key).orElse(defaultValue);
    }

    public Optional<Boolean> getOptionalBoolean(String key) {
        return getOptionalProperty(key, v -> parseBoolean(v, key));
    }

    public boolean getOptionalBoolean(String key, boolean defaultValue) {
        return getOptionalBoolean(key).orElse(defaultValue);
    }

    public Optional<List<String>> getOptionalList(String key) {
        return getOptionalProperty(key, v -> Arrays.asList(v.split(",")));
    }

    public Optional<List<Long>> getOptionalLongList(String key) {
        return getOptionalProperty(key, v -> Arrays.stream(v.split(",")).map(num -> parseLong(num, key)).toList());
    }

    public Optional<Pattern> getOptionalRegex(String key) {
        return getOptionalProperty(key, Pattern::compile);
    }

    public String getPropertyOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    private boolean parseBoolean(String value, String propKey) {
        if(value.equalsIgnoreCase("true")) {
            return true;
        }
        else if(value.equalsIgnoreCase("false")) {
            return false;
        }

        throw new RuntimeException("The property " + propKey + " can only contain 'true' or 'false'!");
    }

    private long parseLong(String value, String propKey) {
        try {
            return Long.parseLong(value, 10);
        } catch (NumberFormatException e) {
            throw new RuntimeException("The property " + propKey + " can only contain numbers!");
        }
    }

    private <T> T getRequiredProperty(String key, Function<String, T> parser) {
        String value = properties.getProperty(key);
        if(value == null) {
            throw new RuntimeException("The required property " + key + " was not found in amcdb.properties!");
        }
        return parser.apply(value);
    }

    private <T> Optional<T> getOptionalProperty(String key, Function<String, T> parser) {
        String value = properties.getProperty(key);
        if(value == null) {
            return Optional.empty();
        }

        return Optional.of(parser.apply(value));
    }

    // Getters below


    @Override
    public OptionalLong getShutdownDelay() {
        return shutdownDelay;
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
    public Optional<Pattern> getDiscordMessageFilterPattern() { return discordMessageFilterPattern; }

    @Override
    public boolean getDiscordMessageFilterExclude() { return discordMessageFilterExclude; }

    @Override
    public Optional<List<String>> getDiscordIgnoredExternalUsers() { return discordIgnoredExternalUsers; }

    @Override
    public boolean getDiscordIgnoreBroadcast() { return discordIgnoreBroadcast; }

    @Override
    public Optional<String> getDiscordLifecycleStartedFormat() { return discordLifecycleStartedFormat; }

    @Override
    public Optional<String> getDiscordLifecycleStoppedFormat() { return discordLifecycleStoppedFormat; }

    @Override
    public OptionalLong getDiscordAlertMsptThreshold() { return discordAlertMsptThreshold; }

    @Override
    public Optional<List<Long>> getDiscordAlertUserIds() { return discordAlertUserIds; }

    @Override
    public Optional<List<Long>> getDiscordAlertRoleIds() { return discordAlertRoleIds; }

    @Override
    public long getDiscordAlertCooldown() { return discordAlertCooldown; }

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
    public Optional<Pattern> getMinecraftMessageFilterPattern() { return minecraftMessageFilterPattern; }

    @Override
    public boolean getMinecraftMessageFilterExclude() { return minecraftMessageFilterExclude; }

    @Override
    public Optional<List<String>> getMinecraftIgnoredExternalUsers() { return minecraftIgnoredExternalUsers; }

    @Override
    public String getMinecraftAvatarApiUrl() { return minecraftAvatarApiUrl; }

    @Override
    public String getMinecraftLogFile() {
        return minecraftLogFile;
    }
}
