package network.parthenon.amcdb.data.services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DiscordRoleServiceTest extends DataServiceTestBase {

    static final UUID TEST_SERVER_UUID = UUID.randomUUID();

    Random rng = new Random();

    @BeforeAll
    public static void setupDatabase() {
        setupDatabase(true);
    }

    /**
     * Tests that when a player is marked online on two servers which grant different online roles,
     * neither server is prevented from removing its online role.
     */
    @Test
    public void testPerServerRoles() {
        UUID otherServerUuid = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();

        long role1 = rng.nextLong();
        long role2 = rng.nextLong();

        DiscordRoleService drs1 = new DiscordRoleService(databaseProxy, TEST_SERVER_UUID);
        DiscordRoleService drs2 = new DiscordRoleService(databaseProxy, otherServerUuid);

        PlayerMappingService pms1 = new PlayerMappingService(databaseProxy, TEST_SERVER_UUID);
        PlayerMappingService pms2 = new PlayerMappingService(databaseProxy, otherServerUuid);

        drs1.registerOnlineRole(role1).join();
        drs2.registerOnlineRole(role2).join();

        pms1.markOnline(playerUuid).join();
        pms2.markOnline(playerUuid).join();

        assertFalse(drs1.checkOtherServersGrantingOnlineRole(playerUuid).join());
        assertFalse(drs2.checkOtherServersGrantingOnlineRole(playerUuid).join());
    }

    /**
     * Tests that when a player is marked online on two servers which grant the same online role,
     * neither server can remove the role until the player is marked offline.
     */
    @Test
    public void testSharedRole() {
        UUID otherServerUuid = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();

        long role = rng.nextLong();

        DiscordRoleService drs1 = new DiscordRoleService(databaseProxy, TEST_SERVER_UUID);
        DiscordRoleService drs2 = new DiscordRoleService(databaseProxy, otherServerUuid);

        PlayerMappingService pms1 = new PlayerMappingService(databaseProxy, TEST_SERVER_UUID);
        PlayerMappingService pms2 = new PlayerMappingService(databaseProxy, otherServerUuid);

        drs1.registerOnlineRole(role).join();
        drs2.registerOnlineRole(role).join();

        pms1.markOnline(playerUuid).join();
        pms2.markOnline(playerUuid).join();

        assertTrue(drs1.checkOtherServersGrantingOnlineRole(playerUuid).join());
        assertTrue(drs2.checkOtherServersGrantingOnlineRole(playerUuid).join());

        pms2.markOffline(playerUuid).join();

        assertFalse(drs1.checkOtherServersGrantingOnlineRole(playerUuid).join());
        assertTrue(drs2.checkOtherServersGrantingOnlineRole(playerUuid).join());
    }
}