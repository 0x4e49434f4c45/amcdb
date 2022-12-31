package network.parthenon.amcdb.config;

import net.fabricmc.loader.api.FabricLoader;
import network.parthenon.amcdb.AMCDB;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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

    public static boolean getRequiredBoolean(String key) {
        try {
            return parseBoolean(getRequiredProperty(key));
        }
        catch(RuntimeException e) {
            throw new RuntimeException("The property " + key + " must be 'true' or 'false'!");
        }
    }

    public static Optional<String> getOptionalProperty(String key) {
        return Optional.ofNullable(PROPERTIES.getProperty(key));
    }

    public static Optional<Long> getOptionalLong(String key) {
        String value = PROPERTIES.getProperty(key);
        if(value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(value, 10));
        } catch (NumberFormatException e) {
            throw new RuntimeException("When the property " + key + " is set, it must be a number!");
        }
    }

    public static long getOptionalLong(String key, long defaultValue) {
        Optional<Long> opt = getOptionalLong(key);
        return opt.isPresent() ? opt.get() : defaultValue;
    }

    public static Optional<Boolean> getOptionalBoolean(String key) {
        String value = PROPERTIES.getProperty(key);
        if(value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(parseBoolean(value));
        }
        catch(RuntimeException e) {
            throw new RuntimeException("When the property " + key + " is set, it must be 'true' or 'false'!");
        }
    }

    public static boolean getOptionalBoolean(String key, boolean defaultValue) {
        Optional<Boolean> opt = getOptionalBoolean(key);
        return opt.isPresent() ? opt.get() : defaultValue;
    }

    public static String getPropertyOrDefault(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static boolean hasProperty(String key) {
        return PROPERTIES.containsKey(key);
    }

    private static boolean parseBoolean(String value) {
        if(value.equalsIgnoreCase("true")) {
            return true;
        }
        else if(value.equalsIgnoreCase("false")) {
            return false;
        }

        throw new RuntimeException("Invalid boolean value");
    }
}
