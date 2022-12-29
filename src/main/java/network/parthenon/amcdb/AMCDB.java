package network.parthenon.amcdb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.discord.DiscordService;
import network.parthenon.amcdb.minecraft.MinecraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		AMCDBConfig.loadConfig();

		MinecraftService.init();
		DiscordService.init();

		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			minecraftServerInstance = server;
		});

		LOGGER.info("AMCDB (Another Minecraft-Discord Bridge) loaded!");
	}

	/**
	 * Gets the MinecraftServer instance.
	 *
	 * CAUTION! May return null if the server is not yet initialized.
	 *
	 * @return MinecraftServer instance, or null if the server is not initialized.
	 */
	public static MinecraftServer getMinecraftServerInstance() {
		return minecraftServerInstance;
	}
}
