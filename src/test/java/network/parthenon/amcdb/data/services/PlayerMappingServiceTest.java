package network.parthenon.amcdb.data.services;

import network.parthenon.amcdb.data.BackgroundConnection;
import network.parthenon.amcdb.data.Connection;
import network.parthenon.amcdb.data.entities.PlayerMapping;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMappingServiceTest {

    private static final String DATABASE_LOCATION = "amcdb.test.sqlite3";

    static Random rng = new Random();

    static Connection dbConnection;

    PlayerMappingService playerMappingService;

    @BeforeAll
    public static void setupDatabase() throws SQLException {
        dbConnection = new BackgroundConnection(
                DriverManager.getConnection("jdbc:sqlite:" + DATABASE_LOCATION),
                "amcdb-test-database");
    }
    @BeforeEach
    public void setup(){
        playerMappingService = new PlayerMappingService(dbConnection);
    }

    @AfterAll
    public static void cleanupDatabase() throws SQLException {
        dbConnection.close();
        new File(DATABASE_LOCATION).delete();
    }

    /**
     * Tests that a mapping is initially created as unconfirmed and is then confirmed when the
     * correct confirmation code is supplied.
     */
    @Test
    public void testMappingConfirmation() {
        UUID uuid = UUID.randomUUID();

        String confCode = playerMappingService.createUnconfirmed(uuid, rng.nextLong()).join();
        assertFalse(playerMappingService.getByMinecraftUuid(uuid, false).join().isConfirmed(),
                "Player mapping is created in unconfirmed state");
        playerMappingService.confirm(uuid, confCode);
        assertTrue(playerMappingService.getByMinecraftUuid(uuid, true).join().isConfirmed(),
                "Player mapping is confirmed after confirmation code is supplied");
    }

    /**
     * Tests that a player mapping is not confirmed when an incorrect confirmation code is supplied
     */
    @Test
    public void testIncorrectConfirmationCode() {
        UUID uuid = UUID.randomUUID();

        String confCode = playerMappingService.createUnconfirmed(uuid, rng.nextLong()).join();
        assertFalse(playerMappingService.getByMinecraftUuid(uuid, false).join().isConfirmed(),
                "Player mapping is created in unconfirmed state");
        playerMappingService.confirm(uuid, "wrongConfirmationCode");
        assertFalse(playerMappingService.getByMinecraftUuid(uuid, false).join().isConfirmed(),
                "Player mapping is not confirmed after incorrect confirmation code is supplied");
    }

    /**
     * Tests that the existing mapping is updated and the confirmation code is reset when the same UUID is created twice.
     */
    @Test
    public void testMappingUpdate() {
        UUID uuid = UUID.randomUUID();
        long oldSnowflake = rng.nextLong();
        long newSnowflake = rng.nextLong();

        String confCode = playerMappingService.createUnconfirmed(uuid, oldSnowflake).join();
        assertEquals(oldSnowflake, playerMappingService.getByMinecraftUuid(uuid, false).join().getDiscordSnowflake());
        playerMappingService.confirm(uuid, confCode);
        playerMappingService.createUnconfirmed(uuid, newSnowflake);
        PlayerMapping newMapping = playerMappingService.getByMinecraftUuid(uuid, false).join();
        assertEquals(newSnowflake, newMapping.getDiscordSnowflake());
        assertFalse(newMapping.isConfirmed(), "Player mapping is unconfirmed after update");
    }
}