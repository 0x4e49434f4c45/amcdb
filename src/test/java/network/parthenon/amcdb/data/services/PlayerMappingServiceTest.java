package network.parthenon.amcdb.data.services;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.DatabaseProxyImpl;
import network.parthenon.amcdb.data.entities.PlayerMapping;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMappingServiceTest {

    static Random rng = new Random();

    static DatabaseProxy databaseProxy;

    PlayerMappingService playerMappingService;

    @BeforeAll
    public static void setupDatabase() throws SQLException {
        databaseProxy = new DatabaseProxyImpl(new JdbcConnectionSource("jdbc:h2:mem:amcdb-test"));
    }
    @BeforeEach
    public void setup(){
        playerMappingService = new PlayerMappingService(databaseProxy);
    }

    @AfterAll
    public static void cleanupDatabase() throws SQLException {
        databaseProxy.close();
    }

    /**
     * Tests that a mapping is initially created as unconfirmed and is then confirmed when the
     * correct confirmation code is supplied.
     */
    @Test
    public void testMappingConfirmation() {
        UUID uuid = UUID.randomUUID();

        String confCode = playerMappingService.createUnconfirmed(uuid, rng.nextLong()).join();
        playerMappingService.confirm(uuid, confCode).join();
        assertTrue(playerMappingService.getByMinecraftUuid(uuid).join().isConfirmed(),
                "Player mapping is confirmed after confirmation code is supplied");
    }

    /**
     * Tests that a player mapping is not confirmed when an incorrect confirmation code is supplied
     */
    @Test
    public void testIncorrectConfirmationCode() {
        UUID uuid = UUID.randomUUID();

        String confCode = playerMappingService.createUnconfirmed(uuid, rng.nextLong()).join();
        playerMappingService.confirm(uuid, "wrongConfirmationCode");
        assertNull(playerMappingService.getByMinecraftUuid(uuid).join(),
                "Player mapping is not confirmed after incorrect confirmation code is supplied");
    }

    /**
     * Tests that the existing mapping is updated when the same UUID is created twice and confirmed twice.
     */
    @Test
    public void testMappingUpdate() {
        UUID uuid = UUID.randomUUID();
        long oldSnowflake = rng.nextLong();
        long newSnowflake = rng.nextLong();

        String confCode = playerMappingService.createUnconfirmed(uuid, oldSnowflake).join();
        playerMappingService.confirm(uuid, confCode).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid).join().getDiscordSnowflake());
        confCode = playerMappingService.createUnconfirmed(uuid, newSnowflake).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid).join().getDiscordSnowflake(),
                "Confirmed player mapping is unchanged after new unconfirmed mapping is created");
        playerMappingService.confirm(uuid, confCode).join();
        assertEquals(newSnowflake, playerMappingService.getByMinecraftUuid(uuid).join().getDiscordSnowflake(),
                "Player mapping is updated once new mapping is confirmed");
    }

    /**
     * Tests that an existing confirmed mapping is not removed when attempting to confirm a new mapping
     * with an incorrect code.
     */
    @Test
    public void testMappingUpdateIncorrectConfirmationCode() {
        UUID uuid = UUID.randomUUID();
        long oldSnowflake = rng.nextLong();
        long newSnowflake = rng.nextLong();

        String confCode = playerMappingService.createUnconfirmed(uuid, oldSnowflake).join();
        playerMappingService.confirm(uuid, confCode).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid).join().getDiscordSnowflake());
        confCode = playerMappingService.createUnconfirmed(uuid, newSnowflake).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid).join().getDiscordSnowflake(),
                "Confirmed player mapping is unchanged after new unconfirmed mapping is created");
        playerMappingService.confirm(uuid, "wrongConfirmationCode").join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid).join().getDiscordSnowflake(),
                "Confirmed player mapping is unchanged after failure to confirm new mapping");
    }

    /**
     * Tests that when a mapping is confirmed, existing unconfirmed mappings are no longer
     * able to be confirmed.
     */
    @Test
    public void testMultipleUnconfirmed() {
        UUID uuid = UUID.randomUUID();
        long snowflake1 = rng.nextLong();
        long snowflake2 = rng.nextLong();

        String confCode1 = playerMappingService.createUnconfirmed(uuid, snowflake1).join();
        String confCode2 = playerMappingService.createUnconfirmed(uuid, snowflake2).join();
        assertTrue(playerMappingService.confirm(uuid, confCode1).join());
        assertEquals(snowflake1, playerMappingService.getByMinecraftUuid(uuid).join().getDiscordSnowflake());
        assertFalse(playerMappingService.confirm(uuid, confCode2).join());
    }

    /**
     * Tests that creating and confirming a mapping does not affect mappings for
     * other UUIDs.
     */
    @Test
    public void testMultipleUuids() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        long snowflake1 = rng.nextLong();
        long snowflake2 = rng.nextLong();

        String confCode1 = playerMappingService.createUnconfirmed(uuid1, snowflake1).join();
        String confCode2 = playerMappingService.createUnconfirmed(uuid2, snowflake2).join();
        assertTrue(playerMappingService.confirm(uuid1, confCode1).join());
        assertTrue(playerMappingService.confirm(uuid2, confCode2).join());
        assertEquals(snowflake1, playerMappingService.getByMinecraftUuid(uuid1).join().getDiscordSnowflake());
        assertEquals(snowflake2, playerMappingService.getByMinecraftUuid(uuid2).join().getDiscordSnowflake());
    }

    /**
     * Tests that all of a player's confirmed and unconfirmed mappings are removed
     * without affecting mappings for other players.
     */
    @Test
    public void testRemoval() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        long snowflake1 = rng.nextLong();
        long snowflake1_2 = rng.nextLong();
        long snowflake2 = rng.nextLong();

        String confCode1 = playerMappingService.createUnconfirmed(uuid1, snowflake1).join();
        assertTrue(playerMappingService.confirm(uuid1, confCode1).join());
        String confCode1_2 = playerMappingService.createUnconfirmed(uuid1, snowflake1_2).join();
        String confCode2 = playerMappingService.createUnconfirmed(uuid2, snowflake2).join();
        assertTrue(playerMappingService.confirm(uuid2, confCode2).join());

        assertEquals(2, playerMappingService.remove(uuid1).join());

        assertFalse(playerMappingService.confirm(uuid1, confCode1_2).join());

        assertNull(playerMappingService.getByMinecraftUuid(uuid1).join());
        assertEquals(snowflake2, playerMappingService.getByMinecraftUuid(uuid2).join().getDiscordSnowflake());
    }
}