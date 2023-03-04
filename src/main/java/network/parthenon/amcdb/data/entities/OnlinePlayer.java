package network.parthenon.amcdb.data.entities;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;
import java.util.UUID;

@DatabaseTable(tableName = "online_players")
public class OnlinePlayer {

    public static final String MINECRAFT_UUID_COLUMN = "minecraft_uuid";
    public static final String SERVER_UUID_COLUMN = "server_uuid";

    @DatabaseField(columnName = MINECRAFT_UUID_COLUMN, canBeNull = false, uniqueCombo = true)
    private UUID minecraftUuid;

    @DatabaseField(columnName = SERVER_UUID_COLUMN, canBeNull = false, uniqueCombo = true)
    private UUID serverUuid;

    public OnlinePlayer(UUID minecraftUuid, UUID serverUuid) {
        this.minecraftUuid = minecraftUuid;
        this.serverUuid = serverUuid;
    }

    /**
     * Package-visible default constructor for use by ORMLite.
     */
    OnlinePlayer() {}

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public UUID getServerUuid() {
        return serverUuid;
    }
    
    @Override
    public boolean equals(Object other) {
        if(super.equals(other)) {
            return true;
        }

        if(!(other instanceof OnlinePlayer)) {
            return false;
        }

        OnlinePlayer otherOp = (OnlinePlayer) other;

        return minecraftUuid.equals(otherOp.minecraftUuid) &&
                serverUuid.equals(otherOp.serverUuid);
    }
}
