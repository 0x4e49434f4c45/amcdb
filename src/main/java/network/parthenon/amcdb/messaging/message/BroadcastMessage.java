package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.InternalMessageComponent;

import java.util.List;

/**
 * A message broadcast by a system (Minecraft or Discord) rather than a user.
 */
public class BroadcastMessage extends InternalMessage {

    public BroadcastMessage(String sourceId, String text) {
        super(sourceId, text);
    }

    public BroadcastMessage(String sourceId, List<? extends InternalMessageComponent> components) {
        super(sourceId, components);
    }
}
