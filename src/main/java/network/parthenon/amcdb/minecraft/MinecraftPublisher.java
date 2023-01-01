package network.parthenon.amcdb.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.MessageHandler;

public class MinecraftPublisher implements MessageHandler {

    @Override
    public void handleMessage(InternalMessage message) {
        MinecraftServer server = AMCDB.getMinecraftServerInstance();
        if(server == null) {
            // Server is not yet initialized. No use broadcasting this message anyway.
            return;
        }

        if(message instanceof ChatMessage) {
            Text minecraftText = MinecraftFormatter.toMinecraftText((ChatMessage) message);
            MinecraftService.getInstance().addRecentlyPublished(minecraftText.getString());
            server.getPlayerManager().broadcast(minecraftText, false);
        }
        else if(message instanceof ConsoleMessage) {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), message.toString());
        }
    }

    @Override
    public String getOwnSourceId() {
        return MinecraftService.MINECRAFT_SOURCE_ID;
    }
}
