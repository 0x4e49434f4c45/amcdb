package network.parthenon.amcdb.data.services;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.TableUtils;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.entities.PlayerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages mappings from Minecraft players to user accounts in other systems (i.e. Discord).
 */
public class PlayerMappingService {

    private DatabaseProxy db;

    /**
     * Used to generate confirmation codes.
     */
    private SecureRandom rng;

    public PlayerMappingService(DatabaseProxy databaseProxy) {
        this.db = databaseProxy;
        // force waiting until the table is created to proceed
        initTable().join();

        rng = new SecureRandom();
    }

    /**
     * Gets the confirmed PlayerMapping with the specified Minecraft UUID, if one exists.
     * @param playerUuid Minecraft player UUID
     * @return PlayerMapping, or null if the UUID was not found.
     */
    public CompletableFuture<PlayerMapping> getByMinecraftUuid(UUID playerUuid) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                return dao.queryBuilder().where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().isNull(PlayerMapping.DISCORD_LINK_CONF_HASH_COLUMN)
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
     * @param confCode   Link confirmation code
     * @return Whether or not confirmation was successful.
     */
    public CompletableFuture<Boolean> confirm(UUID playerUuid, String confCode) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                String confCodeHash = hashConfirmationCode(confCode);
                // first, retrieve unconfirmed mapping if it exists
                PlayerMapping unconfirmedMapping = dao.queryBuilder()
                        .where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.DISCORD_LINK_CONF_HASH_COLUMN, confCodeHash)
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
                        where.or(
                                where.isNull(PlayerMapping.DISCORD_LINK_CONF_HASH_COLUMN),
                                where.not().eq(PlayerMapping.DISCORD_LINK_CONF_HASH_COLUMN, confCodeHash)
                        )
                );
                deleteBuilder.delete();

                // finally, update the unconfirmed mapping to confirmed
                UpdateBuilder<PlayerMapping, ?> updateBuilder = dao.updateBuilder();
                updateBuilder.updateColumnValue(PlayerMapping.DISCORD_LINK_CONF_HASH_COLUMN, null);
                updateBuilder.where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.DISCORD_LINK_CONF_HASH_COLUMN, confCodeHash);
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
     * @param playerUuid The player UUID for which to add or update a mapping.
     * @param snowflake  The player's Discord snowflake (user ID).
     * @return CompletableFuture which will contain confirmation code
     */
    public CompletableFuture<String> createUnconfirmed(UUID playerUuid, long snowflake) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                String confCode = Integer.toString(rng.nextInt(100000, 1000000), 10);
                String confCodeHash = hashConfirmationCode(confCode);
                PlayerMapping existingPm = dao.queryBuilder()
                        .where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                        .and().eq(PlayerMapping.DISCORD_SNOWFLAKE_COLUMN, snowflake)
                        .queryForFirst();

                if(existingPm != null) {
                    // update the existing record
                    UpdateBuilder<PlayerMapping, ?> updateBuilder = dao.updateBuilder()
                            .updateColumnValue(PlayerMapping.DISCORD_LINK_CONF_HASH_COLUMN, confCodeHash);
                    updateBuilder.where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid)
                            .and().eq(PlayerMapping.DISCORD_SNOWFLAKE_COLUMN, snowflake);
                    updateBuilder.update();
                }
                else {
                    dao.create(new PlayerMapping(playerUuid, snowflake, confCodeHash));
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
     * @return CompletableFuture which will contain the number of confirmed and unconfirmed
     *         mappings removed.
     */
    public CompletableFuture<Integer> remove(UUID playerUuid) {
        return db.asyncTransaction(PlayerMapping.class, dao -> {
            try {
                DeleteBuilder<PlayerMapping, ?> deleteBuilder = dao.deleteBuilder();
                deleteBuilder.where().eq(PlayerMapping.MINECRAFT_UUID_COLUMN, playerUuid);
                return deleteBuilder.delete();
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to delete player mapping(s) by UUID", e);
            }
        });
    }

    /**
     * Returns the SHA-256 hash of the provided string, in hexadecimal.
     * @param confCode The confirmation code to hash.
     * @return
     */
    private String hashConfirmationCode(String confCode) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch(NoSuchAlgorithmException e) {
            // this should never happen
            throw new RuntimeException(e);
        }

        byte[] hashBytes = md.digest(confCode.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hashBytes.length * 2);
        for(byte b : hashBytes) {
            sb.append(Character.forDigit((b & 0xFF) >>> 4, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * Creates the player mapping table if it does not exist.
     */
    private CompletableFuture<Void> initTable() {
        return db.asyncTransaction(cs -> {
            try {
                TableUtils.createTableIfNotExists(cs, PlayerMapping.class);
            }
            catch(SQLException e) {
                throw new RuntimeException("Failed to create player mapping table", e);
            }
            return null;
        });
    }
}
