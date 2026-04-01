package network.parthenon.amcdb.minecraft;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.util.PlaceholderFormatter;

//#if MC<11901
//$$ import net.minecraft.server.filter.FilteredMessage;
//$$ import net.minecraft.util.registry.RegistryKey;
//#endif

import java.util.EnumSet;
import java.util.Map;

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
    //#if MC>=11901
    public void handleChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
    //#else
    //$$ public void handleChatMessage(FilteredMessage<SignedMessage> message, ServerPlayerEntity sender, RegistryKey<MessageType> typeKey) {
    //#endif
        InternalMessage internalMessage = new ChatMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                playerToUserReference(sender),
                //#if MC>=11901
                formatter.toComponents(message.decoratedContent())
                //#else
                //$$ formatter.toComponents(message.filtered().getContent())
                //#endif
        );
        broker.publish(internalMessage);
    }

    /**
     * Handler for the Fabric API COMMAND_MESSAGE event.
     */
    //#if MC>=11901
    public void handleCommandMessage(PlayerChatMessage message, CommandSourceStack source, ChatType.Bound params) {
    //#else
    //$$ public void handleCommandMessage(FilteredMessage<SignedMessage> message, ServerCommandSource source, RegistryKey<MessageType> typeKey) {
    //#endif
        InternalMessage internalMessage = new ChatMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                source.isPlayer() ?
                        playerToUserReference(source.getPlayer()) :
                        new EntityReference(source.getTextName()),
                //#if MC>=11901
                formatter.toComponents(message.decoratedContent())
                //#else
                //$$ formatter.toComponents(message.filtered().getContent())
                //#endif
        );
        broker.publish(internalMessage);
    }

    /**
     * Handler for the Fabric API GAME_MESSAGE event.
     */
    //#if MC>=11901
    public void handleGameMessage(MinecraftServer server, Component message, boolean overlay) {
    //#else
    //$$ public void handleGameMessage(Text message, RegistryKey<MessageType> typeKey) {
    //#endif
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

    /**
     * Gets an EntityReference to represent the specified player.
     * @param player The player.
     * @return
     */
    private EntityReference playerToUserReference(ServerPlayer player) {
        //#if MC>=12003
        String playerName = player.getName().getString();
        //#else
        //$$ String playerName = player.getEntityName();
        //#endif
        return new EntityReference(
                player.getStringUUID(),
                playerName,
                playerName,
                MinecraftFormatter.toJavaColor(player.getTeamColor()),
                EnumSet.noneOf(InternalMessageComponent.Style.class),
                playerAvatarUrl(player));
    }

    /**
     * Gets the avatar URL for the specified player based on the avatar API configuration.
     * @param player The player for which to get the avatar URL.
     * @return
     */
    private String playerAvatarUrl(ServerPlayer player) {
        //#if MC>=12003
        String playerName = player.getName().getString();
        //#else
        //$$ String playerName = player.getEntityName();
        //#endif
        return PlaceholderFormatter.formatPlaceholders(config.getMinecraftAvatarApiUrl(),
                Map.of("%playerUuid%", player.getStringUUID(), "%playerName%", playerName));
    }
}
