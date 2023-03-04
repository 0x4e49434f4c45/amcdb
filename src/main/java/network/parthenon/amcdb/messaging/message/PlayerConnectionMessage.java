package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.minecraft.MinecraftService;

public class PlayerConnectionMessage extends InternalMessage {

    private final EntityReference player;

    private final ConnectionEvent event;

    protected PlayerConnectionMessage(EntityReference player, ConnectionEvent event) {
        super(MinecraftService.MINECRAFT_SOURCE_ID,
                "[Connection event: player=%s, type=%s]".formatted(player.getDisplayName(), event.toString()));

        this.player = player;
        this.event = event;
    }

    public EntityReference getPlayer() {
        return player;
    }

    public ConnectionEvent getEvent() {
        return event;
    }

    @Override
    public boolean equals(Object other) {
        if(super.equals(other)) {
            return true;
        }

        if(!(other instanceof PlayerConnectionMessage)) {
            return false;
        }

        PlayerConnectionMessage otherPcm = (PlayerConnectionMessage) other;
        return player.equals(otherPcm.player) &&
                event.equals(otherPcm.event);
    }

    /**
     * Returns a PlayerConnectionMessage representing a join event for the specified player.
     * @param player
     * @return
     */
    public static PlayerConnectionMessage join(EntityReference player) {
        return new PlayerConnectionMessage(player, ConnectionEvent.JOIN);
    }

    /**
     * Returns a PlayerConnectionMessage representing a leave event for the specified player.
     * @param player
     * @return
     */
    public static PlayerConnectionMessage leave(EntityReference player) {
        return new PlayerConnectionMessage(player, ConnectionEvent.LEAVE);
    }

    public static enum ConnectionEvent {
        JOIN,
        LEAVE
    }
}
