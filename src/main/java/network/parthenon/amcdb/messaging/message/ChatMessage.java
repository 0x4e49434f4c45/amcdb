package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;

import java.util.List;

/**
 * A message sent by a user to a chat system (e.g. Minecraft in-game chat or Discord channel).
 *
 * For system-broadcast messages not from a particular user, use {@link BroadcastMessage}.
 */
public class ChatMessage extends InternalMessage {

    /**
     * Message author (user). May be null in the case of a system-generated message.
     */
    private final EntityReference author;

    /**
     * Generates a ChatMessage for the given text, without any formatting.
     *
     * @param author   User originating the message.
     * @param sourceId Message source system (i.e. Discord or Minecraft).
     * @param text     Message text.
     */
    public ChatMessage(String sourceId, EntityReference author, String text) {
        this(sourceId, author, List.of(new TextComponent(text)));
    }

    /**
     * Generates a ChatMessage for the given text, without any formatting.
     *
     * @param author     User originating the message.
     * @param sourceId   Message source system (i.e. Discord or Minecraft).
     * @param components Message contents, with formatting information.
     */
    public ChatMessage(String sourceId, EntityReference author, List<? extends InternalMessageComponent> components) {
        super(sourceId, components);
        if(author == null) {
            throw new IllegalArgumentException("Author may not be null (use BroadcastMessage for system broadcast).");
        }
        this.author = author;
    }

    @Override
    protected List<? extends InternalMessageComponent> getComponentsForPlaceholder(String placeholder) {
        List<? extends InternalMessageComponent> components =
                super.getComponentsForPlaceholder(placeholder);
        if(components == null && placeholder.equalsIgnoreCase("username")) {
            return List.of(getAuthor());
        }

        return components;
    }

    /**
     * Message author (user).
     */
    public EntityReference getAuthor() {
        return author;
    }
}
