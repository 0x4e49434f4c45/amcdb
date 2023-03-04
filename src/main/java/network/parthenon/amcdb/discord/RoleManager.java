package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.data.services.PlayerMappingService;
import network.parthenon.amcdb.messaging.MessageHandler;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.message.PlayerConnectionMessage;
import network.parthenon.amcdb.util.AsyncUtil;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RoleManager implements MessageHandler {

    private final PlayerMappingService playerMappingService;

    private final DiscordService discordService;

    private final DiscordConfig config;

    public RoleManager(PlayerMappingService playerMappingService, DiscordService discordService, DiscordConfig config) {
        this.playerMappingService = playerMappingService;
        this.discordService = discordService;
        this.config = config;
    }

    @Override
    public void handleMessage(InternalMessage message) {
        if(!(message instanceof PlayerConnectionMessage)) {
            return;
        }

        handleMessageAsync((PlayerConnectionMessage) message);
    }

    /**
     * Does the actual work of handling the PlayerConnectionMessage.
     *
     * Separated out primarily for testability.
     * @param message PlayerConnectionMessage
     * @return CompletableFuture
     */
    public CompletableFuture<Void> handleMessageAsync(PlayerConnectionMessage message) {

        EntityReference player = message.getPlayer();

        return switch (message.getEvent()) {
            case JOIN -> updateStatus(player, true);
            case LEAVE -> updateStatus(player, false);
        };
    }

    @Override
    public String getOwnSourceId() {
        return DiscordService.DISCORD_SOURCE_ID;
    }

    private CompletableFuture<Void> updateStatus(EntityReference player, boolean online) {
        if(config.getDiscordInMinecraftServerRole().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        long roleId = config.getDiscordInMinecraftServerRole().orElseThrow();
        return playerMappingService.getByMinecraftUuid(extractUuid(player), DiscordService.DISCORD_SOURCE_ID)
                .exceptionally(AsyncUtil::logError)
                .thenCompose(pm -> {
                    if(pm == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return online ?
                            discordService.addRoleToUser(Long.parseLong(pm.getSourceEntityId(), 10), roleId) :
                            discordService.removeRoleFromUser(Long.parseLong(pm.getSourceEntityId(), 10), roleId);
                });
    }

    private UUID extractUuid(EntityReference player) {
        return UUID.fromString(player.getEntityId());
    }
}
