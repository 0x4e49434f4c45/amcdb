package network.parthenon.amcdb.minecraft;

import net.minecraft.server.MinecraftServer;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.InternalMessage;
import network.parthenon.amcdb.messaging.MessageHandler;

public class MinecraftPublisher implements MessageHandler {

    @Override
    public void handleMessage(InternalMessage message) {
        MinecraftServer server = AMCDB.getMinecraftServerInstance();
        if(server == null) {
            // Server is not yet initialized. No use broadcasting this message anyway.
            return;
        }

        if(message.getType() == InternalMessage.MessageType.CHAT) {
            server.getPlayerManager()
                    .broadcast(MinecraftFormatter.toMinecraftText(message), false);
        }
    }

    @Override
    public String getOwnSourceId() {
        return MinecraftService.MINECRAFT_SOURCE_ID;
    }
}
