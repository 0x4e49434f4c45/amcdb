package network.parthenon.amcdb.data.entities;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import java.beans.ConstructorProperties;
import java.util.Arrays;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * Represents a mapping between a Minecraft player and user entities in other system(s).
 */
public class PlayerMapping {

    public static final Table<Record> TABLE = table(name("player_mapping"));
    private static final String MINECRAFT_UUID_COLUMN = "minecraft_uuid";
    public static final Field<UUID> MINECRAFT_UUID = field(name(MINECRAFT_UUID_COLUMN), SQLDataType.UUID);
    private static final String SOURCE_ID_COLUMN = "source_id";
    public static final Field<String> SOURCE_ID = field(name(SOURCE_ID_COLUMN), SQLDataType.VARCHAR);
    private static final String SOURCE_ENTITY_ID_COLUMN = "source_entity_id";
    public static final Field<String> SOURCE_ENTITY_ID = field(name(SOURCE_ENTITY_ID_COLUMN), SQLDataType.VARCHAR);
    private static final String CONFIRMATION_HASH_COLUMN = "confirmation_hash";
    public static final Field<byte[]> CONFIRMATION_HASH = field(name(CONFIRMATION_HASH_COLUMN), SQLDataType.BLOB);

    private UUID minecraftUuid;

    private String sourceId;

    private String sourceEntityId;

    private byte[] confirmationHash;

    /**
     * Creates a new player mapping.
     * @param minecraftUuid    UUID of Minecraft player account.
     * @param sourceId         ID of the system this mapping relates to (e.g. "discord").
     * @param sourceEntityId   ID of the mapped account/entity
     * @param confirmationHash Hashed confirmation code (null for a confirmed mapping).
     */
    @ConstructorProperties({MINECRAFT_UUID_COLUMN, SOURCE_ID_COLUMN, SOURCE_ENTITY_ID_COLUMN, CONFIRMATION_HASH_COLUMN})
    public PlayerMapping(UUID minecraftUuid, String sourceId, String sourceEntityId, byte[] confirmationHash) {
        this.minecraftUuid = minecraftUuid;
        this.sourceId = sourceId;
        this.sourceEntityId = sourceEntityId;
        this.confirmationHash = confirmationHash;
    }

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
