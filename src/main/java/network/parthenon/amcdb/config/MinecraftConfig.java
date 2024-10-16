package network.parthenon.amcdb.config;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public interface MinecraftConfig {

    boolean getMinecraftTextColorsEnabled();

    String getMinecraftMessageFormat();

    Optional<Pattern> getMinecraftMessageFilterPattern();

    boolean getMinecraftMessageFilterExclude();

    Optional<List<String>> getMinecraftIgnoredExternalUsers();

    String getMinecraftAvatarApiUrl();

    String getMinecraftLogFile();
}
