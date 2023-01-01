package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.utils.TimeFormat;
import network.parthenon.amcdb.messaging.component.DateComponent;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.ComponentUtils;
import network.parthenon.amcdb.messaging.component.SplittableInternalMessageComponent;

import java.util.*;

class MarkdownBuilder {

    /**
     * Characters that must be escaped in order to display correctly in Discord.
     */
    private static final List<Character> ESCAPE_CHARS = List.of('\\', '*', '_', '~', '<', '>', '@', '&', '#', ':');

    private static final Map<InternalMessageComponent.Style, String> STYLE_CODES =
            Collections.unmodifiableMap(
                    new EnumMap<>(InternalMessageComponent.Style.class) {{
                        put(InternalMessageComponent.Style.BOLD, "**");
                        put(InternalMessageComponent.Style.ITALIC, "*");
                        put(InternalMessageComponent.Style.UNDERLINE, "__");
                        put(InternalMessageComponent.Style.STRIKETHROUGH, "~~");
                    }}
            );

    private final int maxLength;

    private StringBuilder markdown = new StringBuilder();

    private int styleLength;

    private ArrayDeque<InternalMessageComponent.Style> appliedStyles = new ArrayDeque<>();

    public MarkdownBuilder(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * Appends a new component to the markdown text.
     *
     * If the whole component will not fit in the available space,
     * appends what it can and returns the remainder in a SplittableInternalMessageComponent.
     *
     * @param component The component to append.
     * @return Un-appended remainder, or null if the entire component was appended.
     */
    public SplittableInternalMessageComponent appendSplittableComponent(
            SplittableInternalMessageComponent component) {
        if(!this.applyStyleSet(component.getStyles())) {
            return component;
        }

        String newText = component.getText();
        for(int i = 0; i < newText.length(); i++) {
            boolean needsEscape = ESCAPE_CHARS.contains(newText.charAt(i));

            if(!canFit(needsEscape ? 2 : 1)) {
                return component.split(i);
            }
            if(needsEscape) {
                markdown.append('\\');
            }
            markdown.append(newText.charAt(i));
        }

        // we fit the whole thing
        return null;
    }

    /**
     * Appends a new component to the markdown text, if it can fit.
     * @param component The component to append.
     * @return True if the component was appended, false if not.
     */
    public boolean appendComponent(InternalMessageComponent component) {
        if(!this.applyStyleSet(component.getStyles())) {
            return false;
        }

        String text;
        if(component instanceof DateComponent) {
            DateComponent dateComponent = (DateComponent) component;
            if(dateComponent.getDateFormat() == DateComponent.DateFormat.ABSOLUTE) {
                text = TimeFormat.DATE_TIME_SHORT.atTimestamp(dateComponent.getTimestamp()).toString();
            }
            else {
                text = TimeFormat.RELATIVE.atTimestamp(dateComponent.getTimestamp()).toString();
            }
        }
        else {
            text = escapeMarkdown(component.getText());
        }

        if(!canFit(text.length())) {
            return false;
        }

        markdown.append(text);
        return true;
    }

    public int length() {
        return markdown.length() + styleLength;
    }

    @Override
    public String toString() {
        return markdown.toString() + getClosingStyleCodes();
    }

    private boolean applyStyleSet(Set<InternalMessageComponent.Style> newStyles) {
        EnumSet<InternalMessageComponent.Style> stylesToRemove =
                ComponentUtils.copyStyleSet(appliedStyles);
        stylesToRemove.retainAll(newStyles);

        while(!stylesToRemove.isEmpty()) {
            stylesToRemove.remove(popStyle());
        }

        EnumSet<InternalMessageComponent.Style> stylesToAdd =
                ComponentUtils.copyStyleSet(newStyles);
        stylesToAdd.removeAll(appliedStyles);

        for(InternalMessageComponent.Style style : stylesToAdd) {
            if(!pushStyle(style)) {
                // failed to apply style; likely ran out of room
                return false;
            }
        }

        return true;
    }

    private InternalMessageComponent.Style popStyle() {
        InternalMessageComponent.Style removedStyle = appliedStyles.pop();
        String styleCode = STYLE_CODES.get(removedStyle);
        markdown.append(styleCode);
        styleLength -= styleCode.length();
        return removedStyle;
    }

    private boolean pushStyle(InternalMessageComponent.Style style) {
        String styleCode = STYLE_CODES.get(style);
        if(styleCode == null) {
            // unsupported style; just pretend we applied it
            return true;
        }

        // any style we apply, we have to be able to un-apply without overrunning
        // the maximum length.
        if(!canFit(styleCode.length() * 2)) {
            // style can't be applied without running out of room
            return false;
        }

        // actually apply the style
        markdown.append(styleCode);
        styleLength += styleCode.length();
        appliedStyles.push(style);

        return true;
    }

    private String getClosingStyleCodes() {
        StringBuilder sb = new StringBuilder(16);

        appliedStyles.descendingIterator().forEachRemaining(s -> sb.append(STYLE_CODES.get(s)));

        return sb.toString();
    }

    private boolean canFit(int numChars) {
        return markdown.length() + styleLength + numChars < maxLength;
    }

    public static String escapeMarkdown(String unescapedText) {
        return unescapedText.replaceAll("/([\\\\*_~<>@#&:|])/", "\\$1");
    }
}
