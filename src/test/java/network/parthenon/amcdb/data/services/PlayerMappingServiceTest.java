package network.parthenon.amcdb.data.services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.DatabaseProxyImpl;
import network.parthenon.amcdb.data.entities.OnlinePlayer;
import network.parthenon.amcdb.data.schema.Migration;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMappingServiceTest extends DataServiceTestBase {

    static final String TEST_SOURCE_ID = "JUnit Test";

    static final String TEST_SOURCE_ID_2 = "JUnit Test (Evil Twin)";

    static final UUID TEST_SERVER_UUID = UUID.randomUUID();

    static Random rng = new Random();

    PlayerMappingService playerMappingService;

    @BeforeAll
    public static void setupDatabase() throws SQLException {
        setupDatabase(true);
    }

    @BeforeEach
    public void setup(){
        playerMappingService = new PlayerMappingService(databaseProxy, TEST_SERVER_UUID);
    }

    /**
     * Tests that a mapping is initially created as unconfirmed and is then confirmed when the
     * correct confirmation code is supplied.
     */
    @Test
    public void testMappingConfirmation() {
        UUID uuid = UUID.randomUUID();

        String confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, randomSnowflake()).join();
        playerMappingService.confirm(uuid, TEST_SOURCE_ID, confCode).join();
        assertTrue(playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().isConfirmed(),
                "Player mapping is confirmed after confirmation code is supplied");
    }

    /**
     * Tests that a player mapping is not confirmed when an incorrect confirmation code is supplied
     */
    @Test
    public void testIncorrectConfirmationCode() {
        UUID uuid = UUID.randomUUID();

        String confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, randomSnowflake()).join();
        playerMappingService.confirm(uuid, TEST_SOURCE_ID, "wrongConfirmationCode");
        assertNull(playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join(),
                "Player mapping is not confirmed after incorrect confirmation code is supplied");
    }

    /**
     * Tests that the existing mapping is updated when the same UUID is created twice and confirmed twice.
     */
    @Test
    public void testMappingUpdate() {
        UUID uuid = UUID.randomUUID();
        String oldSnowflake = randomSnowflake();
        String newSnowflake = randomSnowflake();

        String confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, oldSnowflake).join();
        playerMappingService.confirm(uuid, TEST_SOURCE_ID, confCode).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId());
        confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, newSnowflake).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId(),
                "Confirmed player mapping is unchanged after new unconfirmed mapping is created");
        playerMappingService.confirm(uuid, TEST_SOURCE_ID, confCode).join();
        assertEquals(newSnowflake, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId(),
                "Player mapping is updated once new mapping is confirmed");
    }

    /**
     * Tests that an existing confirmed mapping is not removed when attempting to confirm a new mapping
     * with an incorrect code.
     */
    @Test
    public void testMappingUpdateIncorrectConfirmationCode() {
        UUID uuid = UUID.randomUUID();
        String oldSnowflake = randomSnowflake();
        String newSnowflake = randomSnowflake();

        String confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, oldSnowflake).join();
        playerMappingService.confirm(uuid, TEST_SOURCE_ID, confCode).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId());
        confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, newSnowflake).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId(),
                "Confirmed player mapping is unchanged after new unconfirmed mapping is created");
        playerMappingService.confirm(uuid, TEST_SOURCE_ID, "wrongConfirmationCode").join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId(),
                "Confirmed player mapping is unchanged after failure to confirm new mapping");
    }

    /**
     * Tests that when a mapping is confirmed, existing unconfirmed mappings are no longer
     * able to be confirmed.
     */
    @Test
    public void testMultipleUnconfirmed() {
        UUID uuid = UUID.randomUUID();
        String snowflake1 = randomSnowflake();
        String snowflake2 = randomSnowflake();

        String confCode1 = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, snowflake1).join();
        String confCode2 = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, snowflake2).join();
        assertEquals(snowflake1, playerMappingService.confirm(uuid, TEST_SOURCE_ID, confCode1).join().getSourceEntityId());
        assertEquals(snowflake1, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId());
        assertNull(playerMappingService.confirm(uuid, TEST_SOURCE_ID, confCode2).join());
    }

    /**
     * Tests that creating and confirming a mapping does not affect mappings for
     * other UUIDs.
     */
    @Test
    public void testMultipleUuids() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        String snowflake1 = randomSnowflake();
        String snowflake2 = randomSnowflake();

        String confCode1 = playerMappingService.createUnconfirmed(uuid1, TEST_SOURCE_ID, snowflake1).join();
        String confCode2 = playerMappingService.createUnconfirmed(uuid2, TEST_SOURCE_ID, snowflake2).join();
        assertNotNull(playerMappingService.confirm(uuid1, TEST_SOURCE_ID, confCode1).join());
        assertNotNull(playerMappingService.confirm(uuid2, TEST_SOURCE_ID, confCode2).join());
        assertEquals(snowflake1, playerMappingService.getByMinecraftUuid(uuid1, TEST_SOURCE_ID).join().getSourceEntityId());
        assertEquals(snowflake2, playerMappingService.getByMinecraftUuid(uuid2, TEST_SOURCE_ID).join().getSourceEntityId());
    }

    /**
     * Tests that all of a player's confirmed and unconfirmed mappings are removed
     * without affecting mappings for other players.
     */
    @Test
    public void testRemoval() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        String snowflake1 = randomSnowflake();
        String snowflake1_2 = randomSnowflake();
        String snowflake2 = randomSnowflake();

        String confCode1 = playerMappingService.createUnconfirmed(uuid1, TEST_SOURCE_ID, snowflake1).join();
        assertEquals(snowflake1, playerMappingService.confirm(uuid1, TEST_SOURCE_ID, confCode1).join().getSourceEntityId());
        String confCode1_2 = playerMappingService.createUnconfirmed(uuid1, TEST_SOURCE_ID, snowflake1_2).join();
        String confCode2 = playerMappingService.createUnconfirmed(uuid2, TEST_SOURCE_ID, snowflake2).join();
        assertEquals(snowflake2, playerMappingService.confirm(uuid2, TEST_SOURCE_ID, confCode2).join().getSourceEntityId());

        assertEquals(snowflake1, playerMappingService.remove(uuid1, TEST_SOURCE_ID).join().getSourceEntityId());

        assertNull(playerMappingService.confirm(uuid1, TEST_SOURCE_ID, confCode1_2).join());

        assertNull(playerMappingService.getByMinecraftUuid(uuid1, TEST_SOURCE_ID).join());
        assertEquals(snowflake2, playerMappingService.getByMinecraftUuid(uuid2, TEST_SOURCE_ID).join().getSourceEntityId());
    }

    /**
     * Tests that operations for one source ID do not affect records for another source ID.
     */
    @Test
    public void testMultipleSources() {
        UUID uuid = UUID.randomUUID();

        String snowflake1 = randomSnowflake();
        String snowflake2 = randomSnowflake();

        String confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID, snowflake1).join();
        assertEquals(snowflake1, playerMappingService.confirm(uuid, TEST_SOURCE_ID, confCode).join().getSourceEntityId());
        confCode = playerMappingService.createUnconfirmed(uuid, TEST_SOURCE_ID_2, snowflake2).join();
        assertEquals(snowflake2, playerMappingService.confirm(uuid, TEST_SOURCE_ID_2, confCode).join().getSourceEntityId());

        assertEquals(snowflake1, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId());
        assertEquals(snowflake2, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID_2).join().getSourceEntityId());

        assertEquals(snowflake2, playerMappingService.remove(uuid, TEST_SOURCE_ID_2).join().getSourceEntityId());

        assertEquals(snowflake1, playerMappingService.getByMinecraftUuid(uuid, TEST_SOURCE_ID).join().getSourceEntityId());
    }

    /**
     * Tests that online player records are correctly stored, retrieved, and removed.
     */
    @Test
    public void testOnlineStatus() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        playerMappingService.markOnline(uuid1).join();
        playerMappingService.markOnline(uuid2).join();
        playerMappingService.markOnline(uuid3).join();

        List<OnlinePlayer> onlinePlayers = playerMappingService.getAllOnline().join();
        assertEquals(3, onlinePlayers.size());
        assertTrue(onlinePlayers.contains(new OnlinePlayer(uuid1, TEST_SERVER_UUID)));
        assertTrue(onlinePlayers.contains(new OnlinePlayer(uuid2, TEST_SERVER_UUID)));
        assertTrue(onlinePlayers.contains(new OnlinePlayer(uuid3, TEST_SERVER_UUID)));

        playerMappingService.markOffline(uuid2).join();
        onlinePlayers = playerMappingService.getAllOnline().join();
        assertEquals(2, onlinePlayers.size());
        assertTrue(onlinePlayers.contains(new OnlinePlayer(uuid1, TEST_SERVER_UUID)));
        assertTrue(onlinePlayers.contains(new OnlinePlayer(uuid3, TEST_SERVER_UUID)));

        playerMappingService.markAllOffline().join();
        onlinePlayers = playerMappingService.getAllOnline().join();
        assertEquals(0, onlinePlayers.size());
    }

    /**
     * Tests that duplicate status updates are correctly indicated
     * and do not result in duplicate online player records.
     */
    @Test
    public void testDuplicateStatusUpdate() {
        UUID uuid = UUID.randomUUID();

        assertTrue(playerMappingService.markOnline(uuid).join());
        assertFalse(playerMappingService.markOnline(uuid).join());

        assertEquals(1, playerMappingService.getAllOnline().join().size());

        assertTrue(playerMappingService.markOffline(uuid).join());
        assertFalse(playerMappingService.markOffline(uuid).join());

        assertEquals(0, playerMappingService.getAllOnline().join().size());
    }

    /**
     * Tests that marking a player on/offline on one server does not affect player status
     * on other server(s).
     */
    @Test
    public void testMultiServerPlayerStatus() {
        UUID uuid = UUID.randomUUID();

        PlayerMappingService playerMappingService2 = new PlayerMappingService(databaseProxy, UUID.randomUUID());

        // mark player as online on both servers
        playerMappingService.markOnline(uuid).join();
        playerMappingService2.markOnline(uuid).join();

        // each server should report 1 player online
        assertEquals(1, playerMappingService.getAllOnline().join().size());
        assertEquals(1, playerMappingService2.getAllOnline().join().size());

        // mark player offline on first server
        assertEquals(1, playerMappingService.markAllOffline().join().size());

        // now only server 2 should have an online player
        assertEquals(0, playerMappingService.getAllOnline().join().size());
        assertEquals(1, playerMappingService2.getAllOnline().join().size());
    }

    private String randomSnowflake() {
        return Long.toString(rng.nextLong(), 10);
    }
}