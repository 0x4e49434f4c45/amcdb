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
     * Sets the server UUID.
     * @param uuid
     */
    void setServerUuid(UUID uuid);

    /**
     * Stores the current property values to disk.
     */
    void store();
}
