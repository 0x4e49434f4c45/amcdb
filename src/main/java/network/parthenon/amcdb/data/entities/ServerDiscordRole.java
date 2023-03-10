package network.parthenon.amcdb.data.entities;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import java.beans.ConstructorProperties;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.name;

public class ServerDiscordRole {

    private static final String TABLE_NAME = "server_discord_roles";
    public static final Table<Record> TABLE = table(name(TABLE_NAME));

    private static final String DISCORD_ROLE_SNOWFLAKE_COLUMN = "discord_role_snowflake";
    public static final Field<Long> DISCORD_ROLE_SNOWFLAKE = field(name(TABLE_NAME, DISCORD_ROLE_SNOWFLAKE_COLUMN), SQLDataType.BIGINT);
    private static final String SERVER_UUID_COLUMN = "server_uuid";
    public static final Field<UUID> SERVER_UUID = field(name(TABLE_NAME, SERVER_UUID_COLUMN), SQLDataType.UUID);

    private long discordRoleSnowflake;

    private UUID serverUuid;

    @ConstructorProperties({DISCORD_ROLE_SNOWFLAKE_COLUMN, SERVER_UUID_COLUMN})
    public ServerDiscordRole(long discordRoleSnowflake, UUID serverUuid) {
        this.discordRoleSnowflake = discordRoleSnowflake;
        this.serverUuid = serverUuid;
    }

    public long getDiscordRoleSnowflake() {
        return discordRoleSnowflake;
    }

    public UUID getServerUuid() {
        return serverUuid;
    }

    @Override
    public boolean equals(Object other) {
        if(super.equals(other)) {
            return true;
        }

        if(!(other instanceof ServerDiscordRole)) {
            return false;
        }

        ServerDiscordRole otherSdr = (ServerDiscordRole) other;

        return discordRoleSnowflake == otherSdr.discordRoleSnowflake &&
                serverUuid.equals(otherSdr.serverUuid);
    }
}
