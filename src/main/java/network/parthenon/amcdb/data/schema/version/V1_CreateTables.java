package network.parthenon.amcdb.data.schema.version;

import network.parthenon.amcdb.data.schema.Migration;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.DSL.*;
public class V1_CreateTables extends Migration {
    @Override
    protected String describe() {
        return "Create tables";
    }

    @Override
    protected void apply(Configuration conf) {

        conf.dsl().createTable(name("player_mapping"))
                .column("minecraft_uuid", SQLDataType.UUID.notNull())
                .column("source_id", SQLDataType.VARCHAR.notNull())
                .column("source_entity_id", SQLDataType.VARCHAR.notNull())
                .column("confirmation_hash", SQLDataType.BLOB.null_())
                .unique("minecraft_uuid", "source_id", "source_entity_id")
                .execute();

        conf.dsl().createTable(name("online_players"))
                .column("minecraft_uuid", SQLDataType.UUID.notNull())
                .column("server_uuid", SQLDataType.UUID.notNull())
                .unique("minecraft_uuid", "server_uuid")
                .execute();

        conf.dsl().createTable(name("server_discord_roles"))
                .column("server_uuid", SQLDataType.UUID.notNull())
                .column("discord_role_snowflake", SQLDataType.BIGINT.notNull())
                .unique("server_uuid")
                .execute();
    }
}
