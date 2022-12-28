package network.parthenon.amcdb.minecraft;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import network.parthenon.amcdb.messaging.InternalMessage;
import network.parthenon.amcdb.messaging.ThreadPoolMessageBroker;
import network.parthenon.amcdb.messaging.UserReference;

public class InGameMessageHandler {

    public static final String SOURCE_ID = "minecraft";

    /**
     * Handler for the Fabric API CHAT_MESSAGE event.
     */
    @SuppressWarnings("unused")
    public static void handleChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        InternalMessage internalMessage = new InternalMessage(
                SOURCE_ID,
                playerToUserReference(sender),
                MinecraftFormatter.toComponents(message.getContent())
        );
        ThreadPoolMessageBroker.publish(internalMessage);
    }

    /**
     * Handler for the Fabric API COMMAND_MESSAGE event.
     */
    @SuppressWarnings("unused")
    public static void handleCommandMessage(SignedMessage message, ServerCommandSource source, MessageType.Parameters params) {
        InternalMessage internalMessage = new InternalMessage(
                SOURCE_ID,
                source.isExecutedByPlayer() ?
                        playerToUserReference(source.getPlayer()) :
                        new UserReference(source.getName()),
                MinecraftFormatter.toComponents(message.getContent())
        );
        ThreadPoolMessageBroker.publish(internalMessage);
    }

    /**
     * Handler for the Fabric API GAME_MESSAGE event.
     */
    @SuppressWarnings("unused")
    public static void handleGameMessage(MinecraftServer server, Text message, boolean overlay) {
        InternalMessage internalMessage = new InternalMessage(
                SOURCE_ID,
                null,
                MinecraftFormatter.toComponents(message)
        );
        ThreadPoolMessageBroker.publish(internalMessage);
    }

    private static UserReference playerToUserReference(ServerPlayerEntity player) {
        return new UserReference(
                player.getUuidAsString(),
                player.getEntityName(),
                MinecraftFormatter.toJavaColor(player.getTeamColorValue()));
    }
}
