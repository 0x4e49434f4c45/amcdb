package network.parthenon.amcdb.data.services;

import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.TableUtils;
import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.entities.OnlinePlayer;
import network.parthenon.amcdb.data.entities.PlayerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages mappings from Minecraft players to user accounts in other systems (i.e. Discord).
 */
public class PlayerMappingService {

    private final DatabaseProxy db;

    /**
     * Used to generate confirmation codes.
     */
    private final SecureRandom rng;

    /**
     * Uniquely identifies this server among multiple servers using the database.
     */
    private final UUID serverUuid;

    public PlayerMappingService(DatabaseProxy databaseProxy, UUID serverUuid) {
        this.db = databaseProxy;
        // force waiting until the table is created to proceed
        initTable().join();

        rng = new SecureRandom();

        this.serverUuid = serverUuid;
    }

    /**
     * Gets the confirmed PlayerMapping with the specified Minecraft UUID, if one exists.
     * @param playerUuid Minecraft player UUID.
     * @param sourceId   Source ID of the system for which to get mapping.
     * @return PlayerMapping, or null if the UUID was not found.
     */
    public CompletableFuture<PlayerMapping> getByMinecraftUuid(UUID playerUuid, String sourceId) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                return dao.queryBuilder().where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.SOURCE_ID_COLUMN, sourceId)
                        .and().isNull(PlayerMapping.CONF_HASH_COLUMN)
                        .queryForFirst();
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to query for player mapping by UUID", e);
            }
        });
    }

    /**
     * Sets the player mapping for the specified player to confirmed, if an unconfirmed mapping is found
     * and the confirmation code is valid. Removes any other confirmed or unconfirmed mappings.
     * @param playerUuid Minecraft player UUID
     * @param sourceId   Source ID of the system for which to confirm a mapping.
     * @param confCode   Link confirmation code
     * @return Whether or not confirmation was successful.
     */
    public CompletableFuture<Boolean> confirm(UUID playerUuid, String sourceId, String confCode) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                byte[] confCodeHash = hashConfirmationCode(confCode);
                // first, retrieve unconfirmed mapping if it exists
                PlayerMapping unconfirmedMapping = dao.queryBuilder()
                        .where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.SOURCE_ID_COLUMN, sourceId)
                        .and().eq(PlayerMapping.CONF_HASH_COLUMN, confCodeHash)
                        .queryForFirst();

                if (unconfirmedMapping == null) {
                    return false;
                }

                // mapping confirmed, now delete the other mapping(s) if any
                DeleteBuilder<PlayerMapping, ?> deleteBuilder = dao.deleteBuilder();
                // the return type of deleteBuilder.where() is Where<PlayerMapping, ?>.
                // this prevents grouping clauses using the below syntax (because
                // Where<PlayerMapping, ?> is not assignable to Where<PlayerMapping, ?>).
                // this is a known issue in ORMLite.
                // to get around this, remove the generic qualifier entirely.
                Where where = deleteBuilder.where();
                where.and(
                        where.eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid),
                        where.eq(PlayerMapping.SOURCE_ID_COLUMN, sourceId),
                        where.or(
                                where.isNull(PlayerMapping.CONF_HASH_COLUMN),
                                where.not().eq(PlayerMapping.CONF_HASH_COLUMN, confCodeHash)
                        )
                );
                deleteBuilder.delete();

                // finally, update the unconfirmed mapping to confirmed
                UpdateBuilder<PlayerMapping, ?> updateBuilder = dao.updateBuilder();
                updateBuilder.updateColumnValue(PlayerMapping.CONF_HASH_COLUMN, null);
                updateBuilder.where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.SOURCE_ID_COLUMN, sourceId)
                        .and().eq(PlayerMapping.CONF_HASH_COLUMN, confCodeHash);
                updateBuilder.update();

                return true;
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to confirm mapping", e);
            }
        });
    }

    /**
     * Inserts an unconfirmed mapping to the database, returning the generated 6-digit
     * confirmation code. The confirmation code cannot be retrieved later!
     * @param playerUuid       The player UUID for which to add or update a mapping.
     * @param sourceId         ID of the system this mapping relates to (e.g. "discord").
     * @param sourceEntityId   ID of the mapped account/entity
     * @return CompletableFuture which will contain confirmation code
     */
    public CompletableFuture<String> createUnconfirmed(UUID playerUuid, String sourceId, String sourceEntityId) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                String confCode = Integer.toString(rng.nextInt(100000, 1000000), 10);
                byte[] confCodeHash = hashConfirmationCode(confCode);
                PlayerMapping existingPm = dao.queryBuilder()
                        .where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.SOURCE_ID_COLUMN, sourceId)
                        .and().eq(PlayerMapping.SOURCE_ENTITY_ID_COLUMN, sourceEntityId)
                        .queryForFirst();

                if(existingPm != null) {
                    // update the existing record
                    UpdateBuilder<PlayerMapping, ?> updateBuilder = dao.updateBuilder()
                            .updateColumnValue(PlayerMapping.CONF_HASH_COLUMN, confCodeHash);
                    updateBuilder.where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                            .and().eq(PlayerMapping.SOURCE_ID_COLUMN, sourceId)
                            .and().eq(PlayerMapping.SOURCE_ENTITY_ID_COLUMN, sourceEntityId);
                    updateBuilder.update();
                }
                else {
                    dao.create(new PlayerMapping(playerUuid, sourceId, sourceEntityId, confCodeHash));
                }

                return confCode;
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to create player mapping", e);
            }
        });
    }

    /**
     * Removes all confirmed and unconfirmed mappings associated with the specified player.
     * @param playerUuid Minecraft player UUID
     * @param sourceId   Source ID of the system for which to remove mappings (e.g. "discord").
     * @return CompletableFuture which will contain the number of confirmed and unconfirmed
     *         mappings removed.
     */
    public CompletableFuture<Integer> remove(UUID playerUuid, String sourceId) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                DeleteBuilder<PlayerMapping, ?> deleteBuilder = dao.deleteBuilder();
                deleteBuilder.where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.SOURCE_ID_COLUMN, sourceId);
                return deleteBuilder.delete();
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to delete player mapping(s) by UUID", e);
            }
        });
    }

    /**
     * Marks the specified player as online.
     * @param playerUuid The player to mark online.
     * @return True if the player's status was updated; false if already marked online.
     */
    public CompletableFuture<Boolean> markOnline(UUID playerUuid) {
        return db.asyncTransaction(OnlinePlayer.class, dao -> {
            try {
                OnlinePlayer existingRecord = dao.queryBuilder()
                        .where().eq(OnlinePlayer.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(OnlinePlayer.SERVER_UUID_COLUMN, serverUuid)
                        .queryForFirst();
                if(existingRecord != null) {
                    return false;
                }
                dao.create(new OnlinePlayer(playerUuid, serverUuid));
                return true;
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to retrieve or create online player record", e);
            }
        });
    }

    /**
     * Marks the specified player as offline.
     * @param playerUuid The player to mark offline.
     * @return True if the player's status was updated; false if already marked offline.
     */
    public CompletableFuture<Boolean> markOffline(UUID playerUuid) {
        return db.asyncTransaction(OnlinePlayer.class, dao -> {
            try {
                DeleteBuilder<OnlinePlayer, ?> deleteBuilder = dao.deleteBuilder();
                deleteBuilder.where().eq(OnlinePlayer.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(OnlinePlayer.SERVER_UUID_COLUMN, serverUuid);
                return deleteBuilder.delete() > 0;
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to remove online player record", e);
            }
        });
    }

    /**
     * Gets all players marked as online on this server.
     *
     * Note: this is not necessarily the list of actually online players,
     * particularly if the server stopped abnormally.
     * @return
     */
    public CompletableFuture<List<OnlinePlayer>> getAllOnline() {
        return db.asyncTransaction(OnlinePlayer.class, dao -> {
            try {
                return dao.queryBuilder().where().eq(OnlinePlayer.SERVER_UUID_COLUMN, serverUuid)
                        .query();
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to retrieve online players", e);
            }
        });
    }

    /**
     * Marks all players for this server as offline.
     *
     * This is useful at server startup to ensure there are no players marked online,
     * in case the server stopped abnormally last time.
     * @return List of players that were marked offline.
     */
    public CompletableFuture<List<OnlinePlayer>> markAllOffline() {
        return db.asyncTransaction(OnlinePlayer.class, dao -> {
            try {
                List<OnlinePlayer> players = dao.queryBuilder()
                        .where().eq(OnlinePlayer.SERVER_UUID_COLUMN, serverUuid)
                        .query();

                DeleteBuilder<OnlinePlayer, ?> deleteBuilder = dao.deleteBuilder();
                deleteBuilder.where().eq(OnlinePlayer.SERVER_UUID_COLUMN, serverUuid);
                deleteBuilder.delete();

                return players;
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to remove online player records", e);
            }
        });
    }

    /**
     * Returns the SHA-256 hash of the provided string, in hexadecimal.
     * @param confCode The confirmation code to hash.
     * @return
     */
    private byte[] hashConfirmationCode(String confCode) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch(NoSuchAlgorithmException e) {
            // this should never happen
            throw new RuntimeException(e);
        }

        return md.digest(confCode.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates the player mapping table if it does not exist.
     */
    private CompletableFuture<Void> initTable() {
        return db.asyncTransaction(cs -> {
            try {
                TableUtils.createTableIfNotExists(cs, PlayerMapping.class);
                TableUtils.createTableIfNotExists(cs, OnlinePlayer.class);
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to create player mapping table", e);
            }
            return null;
        });
    }
}
