package network.parthenon.amcdb.minecraft;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.data.entities.OnlinePlayer;
import network.parthenon.amcdb.data.services.PlayerMappingService;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.message.PlayerConnectionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests (some) functionality of the PlayerConnectionHandler.
 *
 * The Fabric event handler methods cannot be tested as it is not possible to mock
 * ServerPlayerEntity.
 */
class PlayerConnectionHandlerTest {

    private MessageBroker mockBroker;
    private PlayerMappingService mockPlayerMappingService;
    private PlayerConnectionHandler handler;

    @BeforeEach
    public void setUp() {
        MinecraftService mockMinecraftService = Mockito.mock(MinecraftService.class);
        MinecraftConfig mockMinecraftConfig = Mockito.mock(MinecraftConfig.class);
        Mockito.when(mockMinecraftConfig.getMinecraftAvatarApiUrl()).thenReturn("https://fake.avatar.url/");

        mockBroker = Mockito.mock(MessageBroker.class);
        mockPlayerMappingService = Mockito.mock(PlayerMappingService.class);

        handler = new PlayerConnectionHandler(
                mockMinecraftService,
                mockMinecraftConfig,
                mockBroker,
                mockPlayerMappingService);
    }

    /**
     * Tests that a PlayerConnectionMessage of type LEAVE is published for each
     * player marked online when cleanOnlinePlayers() is called.
     */
    @Test
    public void testCleanPlayers() {
        UUID serverUuid = UUID.randomUUID();
        UUID steveUuid = UUID.randomUUID();
        UUID alexUuid = UUID.randomUUID();

        Mockito.when(mockPlayerMappingService.markAllOffline())
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new OnlinePlayer(steveUuid, serverUuid),
                        new OnlinePlayer(alexUuid, serverUuid)
                )));

        handler.cleanOnlinePlayers().join();

        ArgumentCaptor<PlayerConnectionMessage> messageCaptor = ArgumentCaptor.forClass(PlayerConnectionMessage.class);
        Mockito.verify(mockBroker, Mockito.times(2)).publish(messageCaptor.capture());
        assertTrue(messageCaptor.getAllValues().containsAll(List.of(
                PlayerConnectionMessage.leave(new EntityReference(steveUuid.toString())),
                PlayerConnectionMessage.leave(new EntityReference(alexUuid.toString()))
        )));
    }
}