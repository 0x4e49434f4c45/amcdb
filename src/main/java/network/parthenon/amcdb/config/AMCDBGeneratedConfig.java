package network.parthenon.amcdb.config;

import java.util.UUID;

public interface AMCDBGeneratedConfig {

    /**
     * Gets the generated UUID that identifies this server among multiple servers
     * sharing the same database.
     * @return
     */
    UUID getServerUuid();

    /**
     * Gets the database version number.
     * @return
     */
    long getDatabaseVersion();

    /**
     * Sets the server UUID.
     * @param uuid
     */
    void setServerUuid(UUID uuid);

    /**
     * Sets the database version number.
     * @param version
     */
    void setDatabaseVersion(long version);

    /**
     * Stores the current property values to disk.
     */
    void store();
}
