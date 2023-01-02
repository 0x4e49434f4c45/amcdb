package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;

import java.util.List;

/**
 * A message to or from Minecraft console.
 */
public class ConsoleMessage extends InternalMessage {

    /**
     * Author of this ConsoleMessage.
     */
    private final EntityReference author;

    /**
     * Creates an unformatted ConsoleMessage with the specified source ID and text,
     * and no author.
     * @param sourceId Message source ID
     * @param text     Message content
     */
    public ConsoleMessage(String sourceId, String text) {
        super(sourceId, text);
        author = null;
    }

    /**
     * Creates a ConsoleMessage with the specified source ID and formatted content,
     * and no author.
     * @param sourceId   Message source ID
     * @param components Message content
     */
    public ConsoleMessage(String sourceId, List<? extends InternalMessageComponent> components) {
        super(sourceId, components);
        author = null;
    }

    /**
     * Creates an unformatted ConsoleMessage with the specified sourceID, author, and text.
     * @param sourceId Message source ID
     * @param author   Message author
     * @param text     Message content
     */
    public ConsoleMessage(String sourceId, EntityReference author, String text) {
        super(sourceId, text);
        this.author = author;
    }

    /**
     * Creates a ConsoleMessage with the specified source ID, author, and formatted content.
     * @param sourceId   Message source ID
     * @param author     Message author
     * @param components Message content
     */
    public ConsoleMessage(String sourceId, EntityReference author, List<? extends InternalMessageComponent> components) {
        super(sourceId, components);
        this.author = author;
    }

    /**
     * Gets the author of this message, if any.
     *
     * May be null if there is no author, e.g. if the message came from the console logs.
     *
     * @return Author EntityReference, or null.
     */
    public EntityReference getAuthor() {
        return author;
    }
}
