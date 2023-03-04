package network.parthenon.amcdb.config;

import java.util.Optional;
import java.util.OptionalLong;

public interface DiscordConfig {

    String getDiscordBotToken();

    long getDiscordGuildId();

    OptionalLong getDiscordChatChannel();

    Optional<String> getDiscordChatTopicFormat();

    Optional<String> getDiscordChatWebhookUrl();

    OptionalLong getDiscordConsoleChannel();

    Optional<String> getDiscordConsoleTopicFormat();

    boolean getDiscordConsoleExecutionEnabled();

    boolean getDiscordUseServerNicknames();

    String getDiscordChatMessageFormat();

    String getDiscordWebhookChatMessageFormat();

    String getDiscordBroadcastMessageFormat();

    OptionalLong getDiscordInMinecraftServerRole();

    long getDiscordBatchingTimeLimit();

    long getDiscordTopicUpdateInterval();

}
