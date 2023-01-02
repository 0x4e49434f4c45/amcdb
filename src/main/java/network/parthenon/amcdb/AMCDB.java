package network.parthenon.amcdb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.discord.DiscordService;
import network.parthenon.amcdb.minecraft.MinecraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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

	private static MinecraftServer minecraftServerInstance;

	/**
	 * Loads configuration and initializes services.
	 */
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		AMCDBConfig.loadConfig();

		// Can't use a static property for this as with other configurations,
		// as by the time we get here our static final properties have already
		// been initialized!
		Optional<Long> shutdownDelay = AMCDBConfig.getOptionalLong("amcdb.shutdown.delay");

		MinecraftService.init();
		DiscordService.init();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			minecraftServerInstance = server;
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			if(shutdownDelay.isEmpty()) {
				doShutdown();
			}

			// If a delay is configured, perform the delay on another thread.
			// Blocking this thread will simply delay other event handlers we
			// might be waiting on!
			new Thread(
					() -> {
						LOGGER.info("Waiting %d ms to handle final log messages (configurable in amcdb.shutodown.delay)"
								.formatted(shutdownDelay.get()));
						try {
							Thread.sleep(shutdownDelay.get());
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
		MinecraftService.shutdown();
		DiscordService.shutdown();
	}

	/**
	 * Gets the MinecraftServer instance.
	 *
	 * CAUTION! May return null if the server is not yet initialized.
	 * @return MinecraftServer instance, or null if the server is not initialized.
	 */
	public static MinecraftServer getMinecraftServerInstance() {
		return minecraftServerInstance;
	}
}
