package network.parthenon.amcdb.config;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;

abstract class ConfigBase {

    protected Properties properties;

    protected ConfigBase(Properties props) {
        properties = props;
    }

    protected String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if(value == null) {
            throw new RuntimeException("The required property " + key + " was not found in amcdb.properties!");
        }
        return value;
    }

    protected long getRequiredLong(String key) {
        try {
            return Long.parseLong(getRequiredProperty(key), 10);
        } catch (NumberFormatException e) {
            throw new RuntimeException("The property " + key + " must be a number!");
        }
    }

    protected boolean getRequiredBoolean(String key) {
        try {
            return parseBoolean(getRequiredProperty(key));
        }
        catch(RuntimeException e) {
            throw new RuntimeException("The property " + key + " must be 'true' or 'false'!");
        }
    }

    protected Optional<String> getOptionalProperty(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    protected OptionalLong getOptionalLong(String key) {
        String value = properties.getProperty(key);
        if(value == null) {
            return OptionalLong.empty();
        }

        try {
            return OptionalLong.of(Long.parseLong(value, 10));
        } catch (NumberFormatException e) {
            throw new RuntimeException("When the property " + key + " is set, it must be a number!");
        }
    }

    protected long getOptionalLong(String key, long defaultValue) {
        OptionalLong opt = getOptionalLong(key);
        return opt.isPresent() ? opt.orElseThrow() : defaultValue;
    }

    protected Optional<Boolean> getOptionalBoolean(String key) {
        String value = properties.getProperty(key);
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

    protected boolean getOptionalBoolean(String key, boolean defaultValue) {
        Optional<Boolean> opt = getOptionalBoolean(key);
        return opt.isPresent() ? opt.orElseThrow() : defaultValue;
    }

    protected String getPropertyOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    protected boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    protected boolean parseBoolean(String value) {
        if(value.equalsIgnoreCase("true")) {
            return true;
        }
        else if(value.equalsIgnoreCase("false")) {
            return false;
        }

        throw new RuntimeException("Invalid boolean value");
    }
}
