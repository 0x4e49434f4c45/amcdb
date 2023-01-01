package network.parthenon.amcdb.messaging.component;

import org.w3c.dom.Text;

import java.awt.*;
import java.util.EnumSet;

public class TextComponent implements SplittableInternalMessageComponent {

    private final EnumSet<Style> appliedStyles;

    private final String text;

    private final String alternateText;

    private final Color color;

    public TextComponent(String text) {
        this(text, null, null, EnumSet.noneOf(Style.class));
    }

    public TextComponent(String text, String alternateText) {
        this(text, alternateText, null, EnumSet.noneOf(Style.class));
    }

    public TextComponent(String text, String alternateText, Color color) {
        this(text, alternateText, color, EnumSet.noneOf(Style.class));
    }

    public TextComponent(String text, String alternateText, EnumSet<Style> styles) {
        this(text, alternateText, null, styles);
    }

    public TextComponent(String text, String alternateText, Color color, EnumSet<Style> styles) {
        if(text == null) {
            throw new IllegalArgumentException("text may not be null");
        }
        if(styles == null) {
            throw new IllegalArgumentException("styles may not be null");
        }

        this.text = text;
        this.alternateText = alternateText;
        this.color = color;
        this.appliedStyles = styles;
    }

    @Override
    public TextComponent split(int index) {
        return new TextComponent(this.text.substring(index), alternateText, color, appliedStyles);
    }

    @Override
    public TextComponent split(int startIndex, int endIndex) {
        return new TextComponent(this.text.substring(startIndex, endIndex), alternateText, color, appliedStyles);
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getAltText() { return alternateText; }

    @Override
    public EnumSet<Style> getStyles() {
        return ComponentUtils.copyStyleSet(appliedStyles);
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof TextComponent)) {
            return false;
        }

        TextComponent otherComponent = (TextComponent) other;

        return this == otherComponent || (
                this.text.equals(otherComponent.text)
                && (this.alternateText == null && otherComponent.alternateText == null || this.alternateText.equals(((TextComponent) other).alternateText))
                && (this.color == null && otherComponent.color == null || this.color.equals(otherComponent.color))
                && this.appliedStyles.equals(otherComponent.appliedStyles)
                );
    }

    @Override
    public String toString() {
        return "TextComponent{text='%s',alt='%s',color=%s,styles=%s}".formatted(text, alternateText, color, appliedStyles);
    }
}
