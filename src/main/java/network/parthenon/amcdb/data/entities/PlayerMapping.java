package network.parthenon.amcdb.data.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;
import java.util.UUID;

/**
 * Represents a mapping between a Minecraft player and user entities in other system(s).
 */
@DatabaseTable(tableName = "player_mapping")
public class PlayerMapping {

    public static final String MINECRAFT_UUID_COLUMN = "minecraft_uuid";
    public static final String SOURCE_ID_COLUMN = "source_id";
    public static final String SOURCE_ENTITY_ID_COLUMN = "source_entity_id";
    public static final String CONF_HASH_COLUMN = "confirmation_hash";

    @DatabaseField(columnName = MINECRAFT_UUID_COLUMN, canBeNull = false, uniqueCombo = true)
    private UUID minecraftUuid;

    @DatabaseField(columnName = SOURCE_ID_COLUMN, canBeNull = false, uniqueCombo = true)
    private String sourceId;

    @DatabaseField(columnName = SOURCE_ENTITY_ID_COLUMN, canBeNull = false, uniqueCombo = true)
    private String sourceEntityId;

    @DatabaseField(columnName = CONF_HASH_COLUMN, canBeNull = true, dataType = DataType.BYTE_ARRAY)
    private byte[] confirmationHash;

    /**
     * Creates a new player mapping.
     * @param minecraftUuid    UUID of Minecraft player account.
     * @param sourceId         ID of the system this mapping relates to (e.g. "discord").
     * @param sourceEntityId   ID of the mapped account/entity
     * @param confirmationHash Hashed confirmation code (null for a confirmed mapping).
     */
    public PlayerMapping(UUID minecraftUuid, String sourceId, String sourceEntityId, byte[] confirmationHash) {
        this.minecraftUuid = minecraftUuid;
        this.sourceId = sourceId;
        this.sourceEntityId = sourceEntityId;
        this.confirmationHash = confirmationHash;
    }

    /**
     * Package-visible default constructor for use by ORMLite.
     */
    PlayerMapping() {}

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public String getSourceId() { return sourceId; }

    public String getSourceEntityId() {
        return sourceEntityId;
    }

    public byte[] getConfirmationHash() { return confirmationHash; }

    public boolean isConfirmed() {
        return confirmationHash == null;
    }

    @Override
    public boolean equals(Object other) {
        if(super.equals(other)) {
            return true;
        }

        if(!(other instanceof PlayerMapping)) {
            return false;
        }

        PlayerMapping otherPm = (PlayerMapping) other;

        return minecraftUuid.equals(otherPm.minecraftUuid) &&
                sourceId.equals(otherPm.sourceId) &&
                sourceEntityId.equals(otherPm.sourceEntityId) &&
                Arrays.equals(confirmationHash, otherPm.confirmationHash);
    }
}
