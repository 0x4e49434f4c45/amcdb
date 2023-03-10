package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.data.entities.PlayerMapping;
import network.parthenon.amcdb.data.services.DiscordRoleService;
import network.parthenon.amcdb.data.services.PlayerMappingService;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.message.PlayerConnectionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class RoleManagerTest {

    private PlayerMappingService mockPlayerMappingService;

    private DiscordRoleService mockDiscordRoleService;

    private DiscordService mockDiscordService;

    private DiscordConfig mockConfig;

    private RoleManager roleManager;

    @BeforeEach
    public void setUp() {
        mockPlayerMappingService = Mockito.mock(PlayerMappingService.class);
        mockDiscordRoleService = Mockito.mock(DiscordRoleService.class);
        mockDiscordService = Mockito.mock(DiscordService.class);
        Mockito.when(mockDiscordService.addRoleToUser(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(mockDiscordService.removeRoleFromUser(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        mockConfig = Mockito.mock(DiscordConfig.class);

        roleManager = new RoleManager(mockPlayerMappingService, mockDiscordRoleService, mockDiscordService, mockConfig);
    }

    /**
     * Tests that the proper role is added to the player's Discord account on player join,
     * if a role is configured.
     */
    @Test
    public void testPlayerJoin() {
        UUID uuid = UUID.randomUUID();
        Mockito.when(mockConfig.getDiscordInMinecraftServerRole()).thenReturn(OptionalLong.of(5));
        Mockito.when(mockPlayerMappingService.getByMinecraftUuid(uuid, DiscordService.DISCORD_SOURCE_ID))
                .thenReturn(CompletableFuture.completedFuture(new PlayerMapping(uuid, DiscordService.DISCORD_SOURCE_ID, "3456", null)));

        roleManager.updateOnlineRole(uuid, true).join();

        Mockito.verify(mockDiscordService, Mockito.times(1)).addRoleToUser(3456, 5);
    }

    /**
     * Tests that no Discord role is added if none is configured.
     */
    @Test
    public void testPlayerJoinNoRole() {
        UUID uuid = UUID.randomUUID();
        Mockito.when(mockConfig.getDiscordInMinecraftServerRole()).thenReturn(OptionalLong.empty());
        Mockito.when(mockPlayerMappingService.getByMinecraftUuid(uuid, DiscordService.DISCORD_SOURCE_ID))
                .thenReturn(CompletableFuture.completedFuture(new PlayerMapping(uuid, DiscordService.DISCORD_SOURCE_ID, "3456", null)));

        roleManager.updateOnlineRole(uuid, true).join();

        Mockito.verify(mockDiscordService, Mockito.never()).addRoleToUser(Mockito.anyLong(), Mockito.anyLong());
    }

    /**
     * Tests that the proper role is removed from the player's Discord account on player leave,
     * if a role is configured.
     */
    @Test
    public void testPlayerLeave() {
        UUID uuid = UUID.randomUUID();
        Mockito.when(mockConfig.getDiscordInMinecraftServerRole()).thenReturn(OptionalLong.of(5));
        Mockito.when(mockPlayerMappingService.getByMinecraftUuid(uuid, DiscordService.DISCORD_SOURCE_ID))
                .thenReturn(CompletableFuture.completedFuture(new PlayerMapping(uuid, DiscordService.DISCORD_SOURCE_ID, "3456", null)));
        Mockito.when(mockDiscordRoleService.checkOtherServersGrantingOnlineRole(uuid))
                .thenReturn(CompletableFuture.completedFuture(false));

        roleManager.updateOnlineRole(uuid, false).join();

        Mockito.verify(mockDiscordService, Mockito.times(1)).removeRoleFromUser(3456, 5);
    }

    /**
     * Tests that no Discord role is removed if none is configured.
     */
    @Test
    public void testPlayerLeaveNoRole() {
        UUID uuid = UUID.randomUUID();
        Mockito.when(mockConfig.getDiscordInMinecraftServerRole()).thenReturn(OptionalLong.empty());
        Mockito.when(mockPlayerMappingService.getByMinecraftUuid(uuid, DiscordService.DISCORD_SOURCE_ID))
                .thenReturn(CompletableFuture.completedFuture(new PlayerMapping(uuid, DiscordService.DISCORD_SOURCE_ID, "3456", null)));

        roleManager.updateOnlineRole(uuid, false).join();

        Mockito.verify(mockDiscordService, Mockito.never()).addRoleToUser(Mockito.anyLong(), Mockito.anyLong());
    }

    /**
     * Tests that the Discord role is not removed if the player is online on another server granting the same role.
     */
    @Test
    public void testPlayerLeaveWhileOnOtherServer() {
        UUID uuid = UUID.randomUUID();
        Mockito.when(mockConfig.getDiscordInMinecraftServerRole()).thenReturn(OptionalLong.of(5));
        Mockito.when(mockPlayerMappingService.getByMinecraftUuid(uuid, DiscordService.DISCORD_SOURCE_ID))
                .thenReturn(CompletableFuture.completedFuture(new PlayerMapping(uuid, DiscordService.DISCORD_SOURCE_ID, "3456", null)));
        Mockito.when(mockDiscordRoleService.checkOtherServersGrantingOnlineRole(uuid))
                .thenReturn(CompletableFuture.completedFuture(true));

        roleManager.updateOnlineRole(uuid, false).join();

        Mockito.verify(mockDiscordService, Mockito.never()).removeRoleFromUser(Mockito.anyLong(), Mockito.anyLong());
    }
}