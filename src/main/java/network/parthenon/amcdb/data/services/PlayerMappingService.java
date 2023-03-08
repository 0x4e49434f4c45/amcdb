package network.parthenon.amcdb.data.services;

import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.entities.OnlinePlayer;
import network.parthenon.amcdb.data.entities.PlayerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
        return db.asyncTransaction(conf -> conf.dsl().select()
                .from(PlayerMapping.TABLE)
                .where(PlayerMapping.MINECRAFT_UUID.eq(playerUuid))
                .and(PlayerMapping.SOURCE_ID.eq(sourceId))
                .and(PlayerMapping.CONFIRMATION_HASH.isNull())
                .fetchOneInto(PlayerMapping.class));
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
        return db.asyncTransaction(conf -> {
            byte[] confCodeHash = hashConfirmationCode(confCode);
            // first, retrieve unconfirmed mapping if it exists
            PlayerMapping unconfirmedMapping = conf.dsl().select()
                    .from(PlayerMapping.TABLE)
                    .where(PlayerMapping.MINECRAFT_UUID.eq(playerUuid))
                    .and(PlayerMapping.SOURCE_ID.eq(sourceId))
                    .and(PlayerMapping.CONFIRMATION_HASH.eq(confCodeHash))
                    .fetchOneInto(PlayerMapping.class);

            if(unconfirmedMapping == null) {
                return false;
            }

            // mapping confirmed, now delete the other mapping(s) if any
            conf.dsl().deleteFrom(PlayerMapping.TABLE)
                    .where(PlayerMapping.MINECRAFT_UUID.eq(playerUuid))
                    .and(PlayerMapping.SOURCE_ID.eq(sourceId))
                    // delete where the confirmation hash is null OR not equal to confCodeHash
                    // jOOQ will emulate the DISTINCT FROM semantics on DBMS that do not support that syntax
                    .and(PlayerMapping.CONFIRMATION_HASH.isDistinctFrom(confCodeHash))
                    .execute();

            // finally, update the unconfirmed mapping to confirmed
            conf.dsl().update(PlayerMapping.TABLE)
                    .setNull(PlayerMapping.CONFIRMATION_HASH)
                    .where(PlayerMapping.MINECRAFT_UUID.eq(playerUuid))
                    .and(PlayerMapping.SOURCE_ID.eq(sourceId))
                    .and(PlayerMapping.CONFIRMATION_HASH.eq(confCodeHash))
                    .execute();

            return true;
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
        return db.asyncTransaction(conf -> {
            String confCode = Integer.toString(rng.nextInt(100000, 1000000), 10);
            byte[] confCodeHash = hashConfirmationCode(confCode);

            // first try to update existing recrd
            int numUpdated = conf.dsl().update(PlayerMapping.TABLE)
                    .set(PlayerMapping.CONFIRMATION_HASH, confCodeHash)
                    .where(PlayerMapping.MINECRAFT_UUID.eq(playerUuid))
                    .and(PlayerMapping.SOURCE_ID.eq(sourceId))
                    .and(PlayerMapping.SOURCE_ENTITY_ID.eq(sourceEntityId))
                    .execute();

            // if there was no existing record, make one
            if(numUpdated == 0) {
                conf.dsl().insertInto(PlayerMapping.TABLE)
                        .set(PlayerMapping.MINECRAFT_UUID, playerUuid)
                        .set(PlayerMapping.SOURCE_ID, sourceId)
                        .set(PlayerMapping.SOURCE_ENTITY_ID, sourceEntityId)
                        .set(PlayerMapping.CONFIRMATION_HASH, confCodeHash)
                        .execute();
            }

            return confCode;
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
        return db.asyncTransaction(conf ->
                conf.dsl().deleteFrom(PlayerMapping.TABLE)
                        .where(PlayerMapping.MINECRAFT_UUID.eq(playerUuid))
                        .and(PlayerMapping.SOURCE_ID.eq(sourceId))
                        .execute());
    }

    /**
     * Marks the specified player as online.
     * @param playerUuid The player to mark online.
     * @return True if the player's status was updated; false if already marked online.
     */
    public CompletableFuture<Boolean> markOnline(UUID playerUuid) {
        return db.asyncTransaction(conf -> {
            OnlinePlayer existing = conf.dsl().select()
                    .from(OnlinePlayer.TABLE)
                    .where(OnlinePlayer.MINECRAFT_UUID.eq(playerUuid))
                    .and(OnlinePlayer.SERVER_UUID.eq(serverUuid))
                    .fetchOneInto(OnlinePlayer.class);

            if(existing != null) {
                return false;
            }

            conf.dsl().insertInto(OnlinePlayer.TABLE)
                    .set(OnlinePlayer.MINECRAFT_UUID, playerUuid)
                    .set(OnlinePlayer.SERVER_UUID, serverUuid)
                    .execute();

            return true;
        });
    }

    /**
     * Marks the specified player as offline.
     * @param playerUuid The player to mark offline.
     * @return True if the player's status was updated; false if already marked offline.
     */
    public CompletableFuture<Boolean> markOffline(UUID playerUuid) {
        return db.asyncTransaction(conf -> {
            int numDeleted = conf.dsl().deleteFrom(OnlinePlayer.TABLE)
                    .where(OnlinePlayer.MINECRAFT_UUID.eq(playerUuid))
                    .and(OnlinePlayer.SERVER_UUID.eq(serverUuid))
                    .execute();

            return numDeleted > 0;
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
        return db.asyncTransaction(conf ->
                conf.dsl().select().from(OnlinePlayer.TABLE)
                        .where(OnlinePlayer.SERVER_UUID.eq(serverUuid))
                        .fetchInto(OnlinePlayer.class));
    }

    /**
     * Marks all players for this server as offline.
     *
     * This is useful at server startup to ensure there are no players marked online,
     * in case the server stopped abnormally last time.
     * @return List of players that were marked offline.
     */
    public CompletableFuture<List<OnlinePlayer>> markAllOffline() {
        return db.asyncTransaction(conf -> {
            List<OnlinePlayer> players = conf.dsl().select()
                    .from(OnlinePlayer.TABLE)
                    .where(OnlinePlayer.SERVER_UUID.eq(serverUuid))
                    .fetchInto(OnlinePlayer.class);

            conf.dsl().deleteFrom(OnlinePlayer.TABLE)
                    .where(OnlinePlayer.SERVER_UUID.eq(serverUuid))
                    .execute();

            return players;
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
}
