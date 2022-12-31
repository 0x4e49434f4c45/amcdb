package network.parthenon.amcdb.messaging.message;

import java.awt.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class TextComponent implements SplittableInternalMessageComponent {

    private EnumSet<Style> appliedStyles;

    private String text;

    private Color color;

    public TextComponent(String text) {
        this(text, null, EnumSet.noneOf(Style.class));
    }

    public TextComponent(String text, Color color) {
        this(text, color, EnumSet.noneOf(Style.class));
    }

    public TextComponent(String text, EnumSet<Style> styles) {
        this(text, null, styles);
    }

    public TextComponent(String text, Color color, EnumSet<Style> styles) {
        if(text == null) {
            throw new IllegalArgumentException("text may not be null");
        }
        if(styles == null) {
            throw new IllegalArgumentException("styles may not be null");
        }

        this.text = text;
        this.color = color;
        this.appliedStyles = styles;
    }

    @Override
    public SplittableInternalMessageComponent split(int index) {
        return new TextComponent(this.text.substring(index), color, appliedStyles);
    }

    @Override
    public SplittableInternalMessageComponent split(int startIndex, int endIndex) {
        return new TextComponent(this.text.substring(startIndex, endIndex), color, appliedStyles);
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
    public Set<Style> getStyles() {
        return Collections.unmodifiableSet(appliedStyles);
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof TextComponent)) {
            return false;
        }

        TextComponent otherComponent = (TextComponent) other;

        return this == otherComponent || (
                this.text.equals(otherComponent.text)
                && (this.color == null && otherComponent.color == null || this.color.equals(otherComponent.color))
                && this.appliedStyles.equals(otherComponent.appliedStyles)
                );
    }

    @Override
    public String toString() {
        return "TextComponent{text='%s',color=%s,styles=%s}".formatted(text, color, appliedStyles);
    }
}
