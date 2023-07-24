package network.parthenon.amcdb.config;

import java.util.Optional;
import java.util.OptionalLong;

public interface DiscordConfig {

    String getDiscordBotToken();

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

    Optional<String> getDiscordLifecycleStartedFormat();

    Optional<String> getDiscordLifecycleStoppedFormat();

    long getDiscordBatchingTimeLimit();

    long getDiscordTopicUpdateInterval();

}
