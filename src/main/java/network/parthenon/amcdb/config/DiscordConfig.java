package network.parthenon.amcdb.config;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Pattern;

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

    Optional<Pattern> getDiscordMessageFilterPattern();

    boolean getDiscordMessageFilterExclude();

    Optional<List<String>> getDiscordIgnoredExternalUsers();

    boolean getDiscordIgnoreBroadcast();

    Optional<String> getDiscordLifecycleStartedFormat();

    Optional<String> getDiscordLifecycleStoppedFormat();

    OptionalLong getDiscordAlertMsptThreshold();

    Optional<List<Long>> getDiscordAlertUserIds();

    Optional<List<Long>> getDiscordAlertRoleIds();

    long getDiscordAlertCooldown();

    long getDiscordBatchingTimeLimit();

    long getDiscordTopicUpdateInterval();

}
