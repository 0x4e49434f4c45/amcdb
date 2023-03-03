package network.parthenon.amcdb.data.entities;

import java.util.UUID;

/**
 * Represents a mapping between a Minecraft player and user entities in other system(s).
 */
public class PlayerMapping {
    private final UUID minecraftUuid;

    private final long discordSnowflake;

    private final boolean isConfirmed;

    /**
     * Creates a new player mapping.
     * @param minecraftUuid    UUID of Minecraft player account.
     * @param discordSnowflake Snowflake (ID) of Discord user.
     * @param isConfirmed      Whether ownership of the Discord account is verified.
     */
    public PlayerMapping(UUID minecraftUuid, long discordSnowflake, boolean isConfirmed) {
        this.minecraftUuid = minecraftUuid;
        this.discordSnowflake = discordSnowflake;
        this.isConfirmed = isConfirmed;
    }

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public long getDiscordSnowflake() {
        return discordSnowflake;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }
}
