package network.parthenon.amcdb.minecraft;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;
import network.parthenon.amcdb.messaging.component.EntityReference;

public class InGameMessageHandler {

    /**
     * Handler for the Fabric API CHAT_MESSAGE event.
     */
    @SuppressWarnings("unused")
    public static void handleChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        InternalMessage internalMessage = new ChatMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                playerToUserReference(sender),
                MinecraftFormatter.toComponents(message.getContent())
        );
        BackgroundMessageBroker.getInstance().publish(internalMessage);
    }

    /**
     * Handler for the Fabric API COMMAND_MESSAGE event.
     */
    @SuppressWarnings("unused")
    public static void handleCommandMessage(SignedMessage message, ServerCommandSource source, MessageType.Parameters params) {
        InternalMessage internalMessage = new ChatMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                source.isExecutedByPlayer() ?
                        playerToUserReference(source.getPlayer()) :
                        new EntityReference(source.getName()),
                MinecraftFormatter.toComponents(message.getContent())
        );
        BackgroundMessageBroker.getInstance().publish(internalMessage);
    }

    /**
     * Handler for the Fabric API GAME_MESSAGE event.
     */
    @SuppressWarnings("unused")
    public static void handleGameMessage(MinecraftServer server, Text message, boolean overlay) {
        // Skip any message sent by AMCDB.
        if(MinecraftService.getInstance().checkAndConsumeRecentlyPublished(message.getString())) {
            return;
        }

        InternalMessage internalMessage = new BroadcastMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                MinecraftFormatter.toComponents(message)
        );
        BackgroundMessageBroker.getInstance().publish(internalMessage);
    }

    private static EntityReference playerToUserReference(ServerPlayerEntity player) {
        return new EntityReference(
                player.getUuidAsString(),
                player.getEntityName(),
                MinecraftFormatter.toJavaColor(player.getTeamColorValue()));
    }
}
