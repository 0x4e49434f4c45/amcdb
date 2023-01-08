package network.parthenon.amcdb.config;

import java.util.Optional;
import java.util.OptionalLong;

public interface DiscordConfig {

    String getDiscordBotToken();

    OptionalLong getDiscordChatChannel();

    Optional<String> getDiscordChatTopicFormat();

    OptionalLong getDiscordConsoleChannel();

    Optional<String> getDiscordConsoleTopicFormat();

    boolean getDiscordConsoleExecutionEnabled();

    boolean getDiscordUseServerNicknames();

    String getDiscordChatMessageFormat();

    String getDiscordBroadcastMessageFormat();

    long getDiscordBatchingTimeLimit();

    long getDiscordTopicUpdateInterval();

}
