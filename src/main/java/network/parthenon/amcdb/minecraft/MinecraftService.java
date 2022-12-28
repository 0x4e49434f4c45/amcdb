package network.parthenon.amcdb.minecraft;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

public class MinecraftService {
    public static void init() {
        // Subscribe to in game messages
        ServerMessageEvents.CHAT_MESSAGE.register(InGameMessageHandler::handleChatMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(InGameMessageHandler::handleCommandMessage);
        ServerMessageEvents.GAME_MESSAGE.register(InGameMessageHandler::handleGameMessage);

        // TODO: Subscribe to message broker
    }
}
