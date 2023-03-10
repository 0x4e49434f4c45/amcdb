package network.parthenon.amcdb.data.services;

import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.entities.OnlinePlayer;
import network.parthenon.amcdb.data.entities.ServerDiscordRole;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.jooq.impl.DSL.name;

public class DiscordRoleService {
    private final DatabaseProxy databaseProxy;

    private final UUID serverUuid;

    private Long discordRoleSnowflake;

    public DiscordRoleService(DatabaseProxy databaseProxy, UUID serverUuid) {
        this.databaseProxy = databaseProxy;
        this.serverUuid = serverUuid;
    }

    /**
     * Associates a Discord online role snowflake with this server.
     * This indicates that this server grants the specified Discord role to its online players.
     * @param snowflake Discord role ID
     * @return
     */
    public CompletableFuture<Void> registerOnlineRole(long snowflake) {
        discordRoleSnowflake = snowflake;

        return databaseProxy.asyncTransaction(conf -> {
            int numUpdated = conf.dsl().update(ServerDiscordRole.TABLE)
                    .set(ServerDiscordRole.DISCORD_ROLE_SNOWFLAKE, discordRoleSnowflake)
                    .where(ServerDiscordRole.SERVER_UUID.eq(serverUuid))
                    .execute();

            if(numUpdated == 0) {
                conf.dsl().insertInto(ServerDiscordRole.TABLE)
                        .set(ServerDiscordRole.DISCORD_ROLE_SNOWFLAKE, discordRoleSnowflake)
                        .set(ServerDiscordRole.SERVER_UUID, serverUuid)
                        .execute();
            }
        });
    }

    /**
     * Returns whether the specified player is marked online on any other servers
     * that grant the same online player role as this server.
     * This can be used to determine whether to remove the Discord role in a multi-server
     * environment.
     * @return
     */
    public CompletableFuture<Boolean> checkOtherServersGrantingOnlineRole(UUID playerUuid) {
        if(discordRoleSnowflake == null) {
            throw new IllegalStateException("Online role is not registered for this server.");
        }

        return databaseProxy.asyncTransactionResult(conf ->
            conf.dsl().selectCount()
                    .from(ServerDiscordRole.TABLE)
                    .join(OnlinePlayer.TABLE)
                    .on(ServerDiscordRole.SERVER_UUID.eq(OnlinePlayer.SERVER_UUID))
                    .where(ServerDiscordRole.SERVER_UUID.ne(serverUuid))
                    .and(ServerDiscordRole.DISCORD_ROLE_SNOWFLAKE.eq(discordRoleSnowflake))
                    .and(OnlinePlayer.MINECRAFT_UUID.eq(playerUuid))
                    .fetchOne()
                    .value1() > 0
        );
    }
}
