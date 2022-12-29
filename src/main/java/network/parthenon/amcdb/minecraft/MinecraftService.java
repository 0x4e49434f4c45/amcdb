package network.parthenon.amcdb.minecraft;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;

import java.io.File;

public class MinecraftService {

    public static final String MINECRAFT_SOURCE_ID = "Minecraft";

    private static final String LOG_FILE = AMCDBConfig.getRequiredProperty("amcdb.minecraft.logFile");

    public static void init() {
        // Subscribe to in game messages
        ServerMessageEvents.CHAT_MESSAGE.register(InGameMessageHandler::handleChatMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(InGameMessageHandler::handleCommandMessage);
        ServerMessageEvents.GAME_MESSAGE.register(InGameMessageHandler::handleGameMessage);

        // Subscribe to message broker
        BackgroundMessageBroker.subscribe(new MinecraftPublisher());

        // Defer reading log file until mods are fully loaded
        // This will ensure that all message handlers are ready
        ServerLifecycleEvents.SERVER_STARTING.register(e -> {
            // Subscribe to console logs
            LogTailer.watchFile(new File(LOG_FILE));
        });
    }
}
