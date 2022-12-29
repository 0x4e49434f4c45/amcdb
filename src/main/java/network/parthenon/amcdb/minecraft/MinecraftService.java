package network.parthenon.amcdb.minecraft;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import network.parthenon.amcdb.messaging.ThreadPoolMessageBroker;

public class MinecraftService {

    public static final String MINECRAFT_SOURCE_ID = "Minecraft";

    public static void init() {
        // Subscribe to in game messages
        ServerMessageEvents.CHAT_MESSAGE.register(InGameMessageHandler::handleChatMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(InGameMessageHandler::handleCommandMessage);
        ServerMessageEvents.GAME_MESSAGE.register(InGameMessageHandler::handleGameMessage);

        // Subscribe to message broker
        ThreadPoolMessageBroker.subscribe(new MinecraftPublisher());
    }
}
