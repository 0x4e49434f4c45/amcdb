package network.parthenon.amcdb.data.entities;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

/**
 * Represents a mapping between a Minecraft player and user entities in other system(s).
 */
@DatabaseTable(tableName = "player_mapping")
public class PlayerMapping {

    public static final String MINECRAFT_UUID_COLUMN = "minecraft_uuid";
    public static final String DISCORD_SNOWFLAKE_COLUMN = "discord_snowflake";
    public static final String DISCORD_LINK_CONF_HASH_COLUMN = "discord_link_confirmation_hash";

    @DatabaseField(columnName = MINECRAFT_UUID_COLUMN, canBeNull = false, uniqueCombo = true)
    private UUID minecraftUuid;

    @DatabaseField(columnName = DISCORD_SNOWFLAKE_COLUMN, canBeNull = false, uniqueCombo = true)
    private long discordSnowflake;

    @DatabaseField(columnName = DISCORD_LINK_CONF_HASH_COLUMN, canBeNull = true)
    private String confirmationHash;

    /**
     * Creates a new player mapping.
     * @param minecraftUuid    UUID of Minecraft player account.
     * @param discordSnowflake Snowflake (ID) of Discord user.
     * @param confirmationHash Hashed confirmation code (null for a confirmed mapping).
     */
    public PlayerMapping(UUID minecraftUuid, long discordSnowflake, String confirmationHash) {
        this.minecraftUuid = minecraftUuid;
        this.discordSnowflake = discordSnowflake;
        this.confirmationHash = confirmationHash;
    }

    /**
     * Package-visible default constructor for use by ORMLite.
     */
    PlayerMapping() {}

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public long getDiscordSnowflake() {
        return discordSnowflake;
    }

    public String getConfirmationHash() { return confirmationHash; }

    public boolean isConfirmed() {
        return confirmationHash == null;
    }
}
