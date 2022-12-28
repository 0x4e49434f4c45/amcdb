package network.parthenon.amcdb.config;

import net.fabricmc.loader.api.FabricLoader;
import network.parthenon.amcdb.AMCDB;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AMCDBConfig {

    public static final String PROPERTIES_FILE = "amcdb.properties";

    private static final Properties PROPERTIES = new Properties();

    public static void loadConfig() {
        Path propertiesPath = FabricLoader.getInstance().getConfigDir().resolve(PROPERTIES_FILE);
        if(Files.notExists(propertiesPath)) {
            // copy from resources
            try(InputStream rsProperties = AMCDBConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
                Files.copy(rsProperties, propertiesPath);
            } catch (IOException e) {
                AMCDB.LOGGER.error("Failed to write AMCDB properties. Make sure that the server is able to write to the config directory.");
                throw new RuntimeException("Failed to write AMCDB properties file", e);
            }
        }

        try(InputStream propertiesStream = Files.newInputStream(propertiesPath)) {
            PROPERTIES.load(propertiesStream);
        } catch (IOException e) {
            AMCDB.LOGGER.error("Failed to load AMCDB properties. Make sure that the server is able to read the file config/amcdb.properties.");
            throw new RuntimeException("Failed to load AMCDB properties file", e);
        }
    }

    public static String getRequiredProperty(String key) {
        String value = PROPERTIES.getProperty(key);
        if(value == null) {
            throw new RuntimeException("The required property " + key + " was not found in amcdb.properties!");
        }
        return value;
    }

    public static long getRequiredLong(String key) {
        try {
            return Long.parseLong(getRequiredProperty(key), 10);
        } catch (NumberFormatException e) {
            throw new RuntimeException("The property " + key + " must be a number!");
        }
    }

    public static String getOptionalProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String getOptionalProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }
}
