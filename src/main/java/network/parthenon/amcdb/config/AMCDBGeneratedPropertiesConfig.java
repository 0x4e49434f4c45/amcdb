package network.parthenon.amcdb.config;

import network.parthenon.amcdb.AMCDB;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class AMCDBGeneratedPropertiesConfig extends ConfigBase implements AMCDBGeneratedConfig {

    private static final String SERVER_UUID_PROPERTY = "amcdb.serverUuid";

    private static final String DATABASE_VERSION_PROPERTY = "amcdb.database.version";

    /**
     * Properties file path.
     */
    private final Path propertiesPath;

    private final Map<String, String> defaults = Map.of(
            SERVER_UUID_PROPERTY, UUID.randomUUID().toString(),
            DATABASE_VERSION_PROPERTY, "1"
    );

    public AMCDBGeneratedPropertiesConfig(Path propsPath) {
        super(new Properties());
        propertiesPath = propsPath;

        properties.putAll(defaults);

        if(Files.exists(propsPath)) {
            try(InputStream propertiesStream = Files.newInputStream(propertiesPath)) {
                properties.load(propertiesStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load AMCDB generated properties file", e);
            }
        }

        store();
    }

    @Override
    public UUID getServerUuid() {
        return UUID.fromString(properties.getProperty(SERVER_UUID_PROPERTY));
    }

    @Override
    public long getDatabaseVersion() {
        return getRequiredLong(DATABASE_VERSION_PROPERTY);
    }

    @Override
    public void setServerUuid(UUID uuid) {
        properties.put(SERVER_UUID_PROPERTY, uuid.toString());
    }

    @Override
    public void setDatabaseVersion(long version) {
        properties.put(DATABASE_VERSION_PROPERTY, version);
    }

    @Override
    public void store() {
        try(OutputStream os = Files.newOutputStream(propertiesPath, StandardOpenOption.CREATE)) {
            properties.store(os, "The values in this file are generated and used internally by AMCDB. To change AMCDB settings, modify amcdb.properties -- do not modify this file.");
        }
        catch(IOException e) {
            throw new RuntimeException("Failed to store default generated properties", e);
        }
    }
}
