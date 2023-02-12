package network.parthenon.amcdb.minecraft;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.messaging.component.EntityReference;

public class InGameMessageHandler {

    private final MinecraftService minecraftService;

    private final MinecraftConfig config;

    private final MinecraftFormatter formatter;

    private final MessageBroker broker;

    public InGameMessageHandler(MinecraftService minecraftService, MinecraftConfig config, MessageBroker broker) {
        this.minecraftService = minecraftService;
        this.config = config;
        this.formatter = new MinecraftFormatter(minecraftService, config);
        this.broker = broker;
    }

    /**
     * Handler for the Fabric API CHAT_MESSAGE event.
     */
    public void handleChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        InternalMessage internalMessage = new ChatMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                playerToUserReference(sender),
                formatter.toComponents(message.getContent())
        );
        broker.publish(internalMessage);
    }

    /**
     * Handler for the Fabric API COMMAND_MESSAGE event.
     */
    public void handleCommandMessage(SignedMessage message, ServerCommandSource source, MessageType.Parameters params) {
        InternalMessage internalMessage = new ChatMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                source.isExecutedByPlayer() ?
                        playerToUserReference(source.getPlayer()) :
                        new EntityReference(source.getName()),
                formatter.toComponents(message.getContent())
        );
        broker.publish(internalMessage);
    }

    /**
     * Handler for the Fabric API GAME_MESSAGE event.
     */
    public void handleGameMessage(MinecraftServer server, Text message, boolean overlay) {
        // Skip any message sent by AMCDB.
        if(minecraftService.checkAndConsumeRecentlyPublished(message.getString())) {
            return;
        }

        InternalMessage internalMessage = new BroadcastMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                formatter.toComponents(message)
        );
        broker.publish(internalMessage);
    }

    private EntityReference playerToUserReference(ServerPlayerEntity player) {
        return new EntityReference(
                player.getUuidAsString(),
                player.getEntityName(),
                null,
                formatter.toJavaColor(player.getTeamColorValue()));
    }
}
