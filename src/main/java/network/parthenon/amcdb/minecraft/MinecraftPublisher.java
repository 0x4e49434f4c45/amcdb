package network.parthenon.amcdb.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.MessageHandler;

//#if MC<11901
//$$ import net.minecraft.network.message.MessageType;
//#endif

public class MinecraftPublisher implements MessageHandler {

    private final MinecraftService minecraftService;

    private final MinecraftConfig config;

    private final MinecraftFormatter formatter;

    public MinecraftPublisher(MinecraftService minecraftService, MinecraftConfig config) {
        this.minecraftService = minecraftService;
        this.config = config;
        this.formatter = new MinecraftFormatter(minecraftService, config);
    }

    @Override
    public void handleMessage(InternalMessage message) {
        MinecraftServer server = minecraftService.getMinecraftServerInstance();
        if(server == null) {
            // Server is not yet initialized. No use broadcasting this message anyway.
            return;
        }

        if(message instanceof ChatMessage) {
            Text minecraftText = formatter.toMinecraftText((ChatMessage) message);
            minecraftService.addRecentlyPublished(minecraftText.getString());
            //#if MC>=11901
            server.getPlayerManager().broadcast(minecraftText, false);
            //#else
            //$$ server.getPlayerManager().broadcast(minecraftText, MessageType.SYSTEM);
            //#endif
        }
        if(message instanceof BroadcastMessage) {
            Text minecraftText = formatter.toMinecraftText((BroadcastMessage) message);
            minecraftService.addRecentlyPublished(minecraftText.getString());
            //#if MC>=11901
            server.getPlayerManager().broadcast(minecraftText, false);
            //#else
            //$$ server.getPlayerManager().broadcast(minecraftText, MessageType.SYSTEM);
            //#endif
        }
        else if(message instanceof ConsoleMessage) {
            String command = message.getUnformattedContents();
            AMCDB.LOGGER.info("Executing console command from %s user %s (id=%s): %s".formatted(
                    message.getSourceId(),
                    // Log user's tag rather than their display name, as the tag is more permanent
                    ((ConsoleMessage) message).getAuthor().getAlternateName(),
                    ((ConsoleMessage) message).getAuthor().getEntityId(),
                    command
            ));
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
        }
    }

    @Override
    public String getOwnSourceId() {
        return MinecraftService.MINECRAFT_SOURCE_ID;
    }
}
