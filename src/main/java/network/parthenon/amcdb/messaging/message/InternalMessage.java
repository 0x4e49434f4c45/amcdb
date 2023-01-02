package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.SplittableInternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AMCDB internal message representation.
 *
 * Includes all the information needed for formatting in either target system (i.e. Discord or Minecraft).
 */
public abstract class InternalMessage {

    /**
     * Message source system (i.e. Discord or Minecraft).
     */
    protected final String sourceId;

    /**
     * Message contents.
     */
    protected final List<? extends InternalMessageComponent> components;

    /**
     * Generates an InternalMessage for the given text, without any formatting.
     *
     * @param sourceId    Message source system (i.e. Discord or Minecraft).
     * @param text        Message text.
     */
    protected InternalMessage(String sourceId, String text) {
        this(sourceId, List.of(new TextComponent(text)));
    }

    /**
     * Creates a new InternalMessage.
     *
     * @param sourceId    Message source system (i.e. Discord or Minecraft).
     * @param components  Message contents, with formatting information.
     */
    protected InternalMessage(String sourceId, List<? extends InternalMessageComponent> components) {
        if(sourceId == null) {
            throw new IllegalArgumentException("Message source must not be null");
        }
        if(components == null) {
            throw new IllegalArgumentException("Message components list must not be null");
        }
        this.sourceId = sourceId;
        this.components = Collections.unmodifiableList(components);
    }

    /**
     * Message source system (i.e. Discord or Minecraft).
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Message contents.
     */
    public List<? extends InternalMessageComponent> getComponents() {
        return components;
    }

    public String getUnformattedContents() {
        return components.stream()
                .map(InternalMessageComponent::getText)
                .collect(Collectors.joining());
    }

    /**
     * Formats the message to a list of InternalMessageComponents using the specified
     * format, then truncates the result to be no longer than truncLength characters.
     *
     * See {@link #formatToComponents(String)} for more information.
     *
     * @param format Format string with placeholders.
     * @param truncLength Maximum length of output.
     * @param finalComponent A component to add to the end if truncation occurred
     *                       (e.g. an ellipsis) - not included in truncLength.
     *                       null to add nothing.
     * @return Component list.
     */
    public List<InternalMessageComponent> formatToComponents(String format, int truncLength, InternalMessageComponent finalComponent) {
        List<InternalMessageComponent> allComponents = formatToComponents(format);
        List<InternalMessageComponent> truncatedComponents = new ArrayList<>(allComponents.size());
        int currentLength = 0;

        for(InternalMessageComponent component : allComponents) {
            // if we can fit the whole thing, add it
            if(currentLength + component.getText().length() <= truncLength) {
                truncatedComponents.add(component);
                currentLength += component.getText().length();
            }
            else {
                // otherwise, split it if we can
                if(currentLength < truncLength && component instanceof SplittableInternalMessageComponent) {
                    truncatedComponents.add(((SplittableInternalMessageComponent) component).split(0, truncLength - currentLength));
                }
                // check if we need to add the final component
                if(finalComponent != null) {
                    truncatedComponents.add(finalComponent);
                }
                // and end the loop here regardless
                break;
            }
        }

        return truncatedComponents;
    }

    /**
     * Formats the message to a list of InternalMessageComponents using the specified
     * format.
     *
     * Subclasses override getComponentsForPlaceholder() to support additional
     * placeholders for their fields.
     *
     * @param format Format string with placeholders.
     * @return Component list.
     */
    public List<InternalMessageComponent> formatToComponents(String format) {
        List<InternalMessageComponent> components = new ArrayList<>();
        
        for(String token : format.split("(?<=^|[^\\\\])%")) {
            List<? extends InternalMessageComponent> placeholderValue;
            if(token.equals("")) {
                // do nothing
            }
            else if((placeholderValue = getComponentsForPlaceholder(token)) != null) {
                components.addAll(placeholderValue);
            }
            else {
                components.add(new TextComponent(token.replace("\\%", "%")));
            }
        }

        return components;
    }

    /**
     * Gets the list of components to replace the given string, if it is a valid placeholder.
     *
     * @param placeholder The string to replace.
     * @return List of components, or null if the string is not a valid placeholder.
     */
    protected List<? extends InternalMessageComponent> getComponentsForPlaceholder(String placeholder) {
        if(placeholder.equalsIgnoreCase("origin")) {
            return List.of(new TextComponent(getSourceId()));
        }
        else if(placeholder.equalsIgnoreCase("message")) {
            return getComponents();
        }

        // not a placeholder we recognize
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(this.getClass().getSimpleName());
        sb.append("{source=").append(sourceId);
        sb.append(",components=")
                .append(String.join(",", () -> components.stream().map(c -> (CharSequence)c.toString()).iterator()));
        sb.append("}");

        return sb.toString();
    }
}
