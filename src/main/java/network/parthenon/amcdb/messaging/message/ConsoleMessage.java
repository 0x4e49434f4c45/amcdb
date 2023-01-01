package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.InternalMessageComponent;

import java.util.List;

/**
 * A message to or from Minecraft console.
 */
public class ConsoleMessage extends InternalMessage {

    public ConsoleMessage(String sourceId, String text) {
        super(sourceId, text);
    }

    public ConsoleMessage(String sourceId, List<? extends InternalMessageComponent> components) {
        super(sourceId, components);
    }
}
