package network.parthenon.amcdb;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.config.AMCDBGeneratedPropertiesConfig;
import network.parthenon.amcdb.config.AMCDBPropertiesConfig;
import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.DatabaseProxyImpl;
import network.parthenon.amcdb.data.services.PlayerMappingService;
import network.parthenon.amcdb.discord.DiscordService;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.minecraft.MinecraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

public class AMCDB implements ModInitializer {

	/**
	 * The Fabric mod ID of this mod.
	 */
	public static final String MOD_ID = "amcdb";

	/**
	 * SLF4J logger instance.
	 */
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private DatabaseProxy databaseProxy;

	private PlayerMappingService playerMappingService;

	private MinecraftService minecraftService;

	private DiscordService discordService;

	private MessageBroker broker;

	private AMCDBPropertiesConfig config;

	private AMCDBGeneratedPropertiesConfig generatedConfig;

	/**
	 * Loads configuration and initializes services.
	 */
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		setupConfiguration();

		// Initialize database connection
		try {
			databaseProxy = new DatabaseProxyImpl(new JdbcPooledConnectionSource(config.getDatabaseConnectionString()));
		}
		catch(SQLException e) {
			throw new RuntimeException("Failed to connect to database!", e);
		}

		// Create services
		playerMappingService = new PlayerMappingService(databaseProxy, generatedConfig.getServerUuid());
		broker = new BackgroundMessageBroker();
		minecraftService = new MinecraftService(broker, config, playerMappingService);
		discordService = new DiscordService(broker, playerMappingService, config);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			if(config.getShutdownDelay().isEmpty()) {
				doShutdown();
				return;
			}

			// If a delay is configured, perform the delay on another thread.
			// Blocking this thread will simply delay other event handlers we
			// might be waiting on!
			new Thread(
					() -> {
						LOGGER.info("Waiting %d ms to handle final log messages (configurable via amcdb.shutdown.delay property)"
								.formatted(config.getShutdownDelay().orElseThrow()));
						try {
							Thread.sleep(config.getShutdownDelay().orElseThrow());
						} catch(InterruptedException e) {
							// do nothing; in the unusual event that we would wake early
							// from sleep, nothing terrible will happen
						}
						this.doShutdown();
					},
					"AMCDB Shutdown Delay")
					.start();
		});

		LOGGER.info("AMCDB (Another Minecraft-Discord Bridge) loaded!");
	}

	/**
	 * Shuts down services.
	 */
	private void doShutdown() {
		minecraftService.shutdown();
		discordService.shutdown();
		databaseProxy.close();
	}

	/**
	 * Prepares the configuration directory and initializes configuration objects.
	 */
	private void setupConfiguration() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path configSubdir = configDir.resolve(MOD_ID);
		try {
			Files.createDirectories(configSubdir);
		}
		catch(IOException e) {
			throw new RuntimeException("Could not create configuration subdirectory!", e);
		}
		this.config = new AMCDBPropertiesConfig(configDir.resolve("amcdb.properties"));
		this.generatedConfig = new AMCDBGeneratedPropertiesConfig(configSubdir.resolve("amcdb.generated.properties"));
	}
}
