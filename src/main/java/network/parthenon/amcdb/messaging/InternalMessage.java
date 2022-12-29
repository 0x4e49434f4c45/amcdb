package network.parthenon.amcdb.messaging;

/**
 * AMCDB internal message representation.
 *
 * Includes all the information needed for formatting in either target system (i.e. Discord or Minecraft).
 */
public class InternalMessage {

    /**
     * Message source system (i.e. Discord or Minecraft).
     */
    private String sourceId;

    /**
     * Message author (user). May be null in the case of a system-generated message.
     */
    private UserReference author;

    /**
     * Message contents.
     */
    private InternalMessageComponent[] components;

    /**
     * Generates an InternalMessage for the given text, without any formatting.
     *
     * @param sourceId Message source system (i.e. Discord or Minecraft).
     * @param author   Message author (user). May be null in the case of a system-generated message.
     * @param text     Message text.
     */
    public InternalMessage(String sourceId, UserReference author, String text) {
        this(sourceId, author, new InternalMessageComponent[] { new TextComponent(text) });
    }

    /**
     * Creates a new InternalMessage.
     *
     * @param sourceId   Message source system (i.e. Discord or Minecraft).
     * @param author     Message author (user). May be null in the case of a system-generated message.
     * @param components Message contents, with formatting information.
     */
    public InternalMessage(String sourceId, UserReference author, InternalMessageComponent[] components) {
        if(sourceId == null) {
            throw new IllegalArgumentException("Message source must not be null");
        }
        if(components == null) {
            throw new IllegalArgumentException("Message components list must not be null");
        }
        this.sourceId = sourceId;
        this.author = author;
        this.components = components;
    }

    /**
     * Message source system (i.e. Discord or Minecraft).
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Message author (user). May be null in the case of a system-generated message.
     */
    public UserReference getAuthor() {
        return author;
    }

    /**
     * Message contents.
     */
    public InternalMessageComponent[] getComponents() {
        return components;
    }

    /**
     * Gets a text representation of the message contents.
     *
     * It is the caller's responsibility to sanitize this string in an appropriate way.
     *
     * @return Message contents as string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(InternalMessageComponent component : components) {
            sb.append(component.getText());
        }

        return sb.toString();
    }
}
