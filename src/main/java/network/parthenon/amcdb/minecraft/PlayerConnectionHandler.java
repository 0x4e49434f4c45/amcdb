package network.parthenon.amcdb.minecraft;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.data.entities.OnlinePlayer;
import network.parthenon.amcdb.data.services.PlayerMappingService;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.message.PlayerConnectionMessage;
import network.parthenon.amcdb.util.AsyncUtil;

import java.util.concurrent.CompletableFuture;

public class PlayerConnectionHandler {

    private final MessageBroker broker;

    private final MinecraftFormatter formatter;

    private final MinecraftConfig config;

    private final PlayerMappingService playerMappingService;

    public PlayerConnectionHandler(
            MinecraftService minecraftService,
            MinecraftConfig config,
            MessageBroker broker,
            PlayerMappingService playerMappingService) {
        this.broker = broker;
        this.formatter = new MinecraftFormatter(minecraftService, config);
        this.config = config;
        this.playerMappingService = playerMappingService;
    }

    /**
     * Handles the Fabric API ServerPlayConnectionEvents.JOIN event.
     * @param handler
     * @param sender
     * @param server
     */
    public void handlePlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        playerMappingService.markOnline(handler.getPlayer().getUuid())
                .exceptionally(AsyncUtil::logError);
        broker.publish(PlayerConnectionMessage.join(formatter.playerToUserReference(handler.getPlayer())));
    }

    /**
     * Handles the Fabric API ServerPlayConnectionEvents.LEAVE event.
     * @param handler
     * @param server
     */
    public void handlePlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
        playerMappingService.markOffline(handler.getPlayer().getUuid())
            .exceptionally(AsyncUtil::logError);
        broker.publish(PlayerConnectionMessage.leave(formatter.playerToUserReference(handler.getPlayer())));
    }

    /**
     * Marks all players as offline and publishes a PlayerConnectionMessage for each one.
     * @return
     */
    public CompletableFuture<Void> cleanOnlinePlayers() {
        return playerMappingService.markAllOffline()
                .exceptionally(AsyncUtil::logError)
                .thenAccept(players -> {
                    if(players == null) {
                        return;
                    }
                    for(OnlinePlayer p : players) {
                        broker.publish(PlayerConnectionMessage.leave(new EntityReference(p.getMinecraftUuid().toString())));
                    }
                });
    }
}
