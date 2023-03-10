package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.exceptions.HierarchyException;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.data.entities.PlayerMapping;
import network.parthenon.amcdb.data.services.DiscordRoleService;
import network.parthenon.amcdb.data.services.PlayerMappingService;
import network.parthenon.amcdb.util.AsyncUtil;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RoleManager {

    private final PlayerMappingService playerMappingService;

    private final DiscordRoleService discordRoleService;

    private final DiscordService discordService;

    private final DiscordConfig config;

    public RoleManager(
            PlayerMappingService playerMappingService,
            DiscordRoleService discordRoleService,
            DiscordService discordService,
            DiscordConfig config) {
        this.playerMappingService = playerMappingService;
        this.discordRoleService = discordRoleService;
        this.discordService = discordService;
        this.config = config;
    }

    /**
     * Looks up the Discord player mapping for the specified Minecraft player,
     * and assigns/removes the Discord online role if a player mapping is found.
     * @param playerUuid Minecraft player ID
     * @param online     True for online (add role); false for offline (remove role).
     * @return
     */
    public CompletableFuture<Void> updateOnlineRole(UUID playerUuid, boolean online) {
        if(config.getDiscordInMinecraftServerRole().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return playerMappingService.getByMinecraftUuid(playerUuid, DiscordService.DISCORD_SOURCE_ID)
                .exceptionally(AsyncUtil::logError)
                .thenCompose(pm -> pm == null ?
                        CompletableFuture.completedFuture(null) :
                        updateOnlineRole(pm, online));
    }

    /**
     * Given a Discord player mapping, updates the player's Discord online role.
     * @param pm     Discord player mapping
     * @param online True for online (add role); false for offline (remove role).
     * @return
     */
    public CompletableFuture<Void> updateOnlineRole(PlayerMapping pm, boolean online) {
        long roleId = config.getDiscordInMinecraftServerRole().orElseThrow();

        try {
            return online ?
                    discordService.addRoleToUser(Long.parseLong(pm.getSourceEntityId(), 10), roleId)
                            .exceptionally(AsyncUtil::logError) :
                    discordRoleService.checkOtherServersGrantingOnlineRole(pm.getMinecraftUuid())
                            .thenCompose(shouldNotRemoveRole ->
                                    shouldNotRemoveRole ?
                                            CompletableFuture.completedFuture(null) :
                                            discordService.removeRoleFromUser(Long.parseLong(pm.getSourceEntityId(), 10), roleId)
                            )
                            .exceptionally(AsyncUtil::logError);
        }
        catch(HierarchyException e) {
            AMCDB.LOGGER.error("Cannot set the in-server Discord role for the player because the in-server role is higher than your bot's role!\n" +
                    "In your Discord server settings, under Roles, drag your bot role above the configured in-server role.");
            return CompletableFuture.failedFuture(e);
        }
    }
}
