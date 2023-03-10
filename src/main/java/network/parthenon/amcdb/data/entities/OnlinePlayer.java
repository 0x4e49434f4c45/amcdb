package network.parthenon.amcdb.data.entities;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import java.beans.ConstructorProperties;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

public class OnlinePlayer {

    private static final String TABLE_NAME = "online_players";
    public static final Table<Record> TABLE = table(name(TABLE_NAME));

    private static final String MINECRAFT_UUID_COLUMN = "minecraft_uuid";
    public static final Field<UUID> MINECRAFT_UUID = field(name(TABLE_NAME, MINECRAFT_UUID_COLUMN), SQLDataType.UUID);
    private static final String SERVER_UUID_COLUMN = "server_uuid";
    public static final Field<UUID> SERVER_UUID = field(name(TABLE_NAME, SERVER_UUID_COLUMN), SQLDataType.UUID);

    private UUID minecraftUuid;

    private UUID serverUuid;

    @ConstructorProperties({MINECRAFT_UUID_COLUMN, SERVER_UUID_COLUMN})
    public OnlinePlayer(UUID minecraftUuid, UUID serverUuid) {
        this.minecraftUuid = minecraftUuid;
        this.serverUuid = serverUuid;
    }

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
