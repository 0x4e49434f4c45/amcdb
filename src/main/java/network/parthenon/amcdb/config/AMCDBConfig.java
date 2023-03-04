package network.parthenon.amcdb.config;

import java.util.OptionalLong;

public interface AMCDBConfig {

    OptionalLong getShutdownDelay();

    String getDatabaseConnectionString();

}
