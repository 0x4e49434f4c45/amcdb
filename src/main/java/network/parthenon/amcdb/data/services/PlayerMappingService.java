package network.parthenon.amcdb.data.services;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.data.entities.PlayerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Manages mappings from Minecraft players to user accounts in other systems (i.e. Discord).
 */
public class PlayerMappingService extends DataService {

    private static final String TABLE = "player_mappings";
    private static final String UUID_COLUMN = "minecraft_uuid";
    private static final String DISCORD_SNOWFLAKE_COLUMN = "discord_snowflake";
    private static final String DISCORD_LINK_CONF_HASH_COLUMN = "discord_link_confirmation_hash";

    /**
     * Used to generate confirmation codes.
     */
    private SecureRandom rng;

    public PlayerMappingService(Connection dbConnection) {
        super(dbConnection);
        initTable();

        rng = new SecureRandom();
    }

    /**
     * Gets the confirmed PlayerMapping with the specified Minecraft UUID, if one exists.
     * @param playerUuid Minecraft player UUID
     * @return PlayerMapping, or null if the UUID was not found.
     * @throws SQLException
     */
    public PlayerMapping getByMinecraftUuid(UUID playerUuid) {
        return getByMinecraftUuid(playerUuid, true);
    }

    /**
     * Gets the PlayerMapping with the specified Minecraft UUID, if one exists.
     * @param playerUuid Minecraft player UUID
     * @param confirmed  If true, return only confirmed users.
     * @return PlayerMapping, or null if the UUID was not found.
     * @throws SQLException
     */
    public PlayerMapping getByMinecraftUuid(UUID playerUuid, boolean confirmed) {
        String confirmedCondition = confirmed ?
                " AND %s IS NULL".formatted(DISCORD_LINK_CONF_HASH_COLUMN) :
                "";
        try {
            return query(
                    this::retrievePlayerMapping,
                    "SELECT * FROM %s WHERE %s = ?%s".formatted(TABLE, UUID_COLUMN, confirmedCondition),
                    playerUuid.toString());
        }
        catch(SQLException e) {
            AMCDB.LOGGER.error("Error retrieving player by UUID!", e);
            return null;
        }
    }

    /**
     * Sets the player mapping for the specified player to confirmed, if an unconfirmed mapping is found
     * and the confirmation code is valid.
     * @param playerUuid Minecraft player UUID
     * @param confCode   Link confirmation code
     * @return Whether or not confirmation was successful.
     */
    public boolean confirm(UUID playerUuid, String confCode) {
        try {
            int rowsUpdated = execute(
                    "UPDATE %s SET %s = NULL WHERE %s = ? AND %s = ?"
                            .formatted(TABLE, DISCORD_LINK_CONF_HASH_COLUMN, UUID_COLUMN, DISCORD_LINK_CONF_HASH_COLUMN),
                    playerUuid.toString(),
                    hashConfirmationCode(confCode));
            return rowsUpdated > 0;
        }
        catch(SQLException e) {
            throw new RuntimeException("Error confirming player mapping (%s=%s)!".formatted(UUID_COLUMN, playerUuid.toString()), e);
        }
    }

    /**
     * Inserts an unconfirmed mapping to the database, returning the generated 6-digit
     * confirmation code. The confirmation code cannot be retrieved later!
     * @param playerUuid The player UUID for which to add or update a mapping.
     * @param snowflake  The player's Discord snowflake (user ID).
     * @return Confirmation code
     */
    public String createUnconfirmed(UUID playerUuid, long snowflake) {
        String confCode = Integer.toString(rng.nextInt(100000, 1000000), 10);
        String confCodeHash = hashConfirmationCode(confCode);
        AMCDB.LOGGER.info(confCodeHash);

        try {
            execute(
                    "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?) ON CONFLICT DO UPDATE SET %s = ?, %s = ? WHERE %s = ?"
                            .formatted(TABLE, UUID_COLUMN, DISCORD_SNOWFLAKE_COLUMN, DISCORD_LINK_CONF_HASH_COLUMN,
                                    DISCORD_SNOWFLAKE_COLUMN, DISCORD_LINK_CONF_HASH_COLUMN, UUID_COLUMN),
                    playerUuid.toString(),
                    snowflake,
                    confCodeHash,
                    snowflake,
                    confCodeHash,
                    playerUuid.toString());
        }
        catch(SQLException e) {
            throw new RuntimeException(
                    "Failed to create player mapping (%s=%s, %s=%s)"
                            .formatted(UUID_COLUMN, playerUuid, DISCORD_SNOWFLAKE_COLUMN, snowflake),
                    e);
        }

        return confCode;
    }

    /**
     * Gets the next row from a ResultSet as a PlayerMapping object.
     * @param rs The ResultSet from which to retrieve the PlayerMapping.
     * @return PlayerMapping, or null if there are no more rows or the row does not have a valid UUID.
     * @throws SQLException
     */
    private PlayerMapping retrievePlayerMapping(ResultSet rs) {
        try {
            rs.setFetchSize(1);
            if (!rs.next()) {
                return null;
            }

            UUID playerUuid;
            try {
                playerUuid = UUID.fromString(rs.getString(UUID_COLUMN));
            } catch (IllegalArgumentException e) {
                AMCDB.LOGGER.warn("Corrupt player mapping entry! minecraft_uuid=%s\n".formatted(rs.getString(UUID_COLUMN)));
                return null;
            }

            return new PlayerMapping(
                    playerUuid,
                    rs.getLong(DISCORD_SNOWFLAKE_COLUMN),
                    rs.getString(DISCORD_LINK_CONF_HASH_COLUMN) == null);
        }
        catch(SQLException e) {
            AMCDB.LOGGER.error("Error retrieving player mapping from result set!", e);
            return null;
        }
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
    private void initTable() {
        try {
            execute("CREATE TABLE IF NOT EXISTS %s(".formatted(TABLE) +
                    "%s TEXT NOT NULL PRIMARY KEY, ".formatted(UUID_COLUMN) +
                    "%s INT NOT NULL UNIQUE, ".formatted(DISCORD_SNOWFLAKE_COLUMN) +
                    "%s TEXT".formatted(DISCORD_LINK_CONF_HASH_COLUMN) +
                    ") WITHOUT ROWID;");
        }
        catch(SQLException e) {
            throw new RuntimeException("Failed to create the %s table".formatted(TABLE), e);
        }
    }
}
