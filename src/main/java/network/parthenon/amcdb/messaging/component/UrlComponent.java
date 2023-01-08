package network.parthenon.amcdb.messaging.component;

import java.awt.*;
import java.util.EnumSet;

public class UrlComponent implements SplittableInternalMessageComponent {

    private final String url;

    private final String displayText;

    private final Color color;

    private final EnumSet<Style> appliedStyles;

    /**
     * Creates a new UrlComponent with the provided URL as display text,
     * no color, and the default underlined style.
     * @param url The URL to link to.
     */
    public UrlComponent(String url) {
        this(url, url, null, EnumSet.of(Style.UNDERLINE));
    }

    /**
     * Creates a new UrlComponent with no color and the default underlined style.
     * @param url         The URL to link to.
     * @param displayText The text to display.
     */
    public UrlComponent(String url, String displayText) {
        this(url, displayText, null, EnumSet.of(Style.UNDERLINE));
    }

    /**
     * Creates a new UrlComponent.
     * @param url           The URL to link to.
     * @param displayText   The text to display.
     * @param color         The text color.
     * @param appliedStyles The text styles.
     */
    public UrlComponent(String url, String displayText, Color color, EnumSet<Style> appliedStyles) {
        if(url == null) {
            throw new IllegalArgumentException("URL may not be null");
        }
        if(displayText == null) {
            throw new IllegalArgumentException("Display text may not be null");
        }
        if(appliedStyles == null) {
            throw new IllegalArgumentException("Style set may not be null");
        }

        this.url = url;
        this.displayText = displayText;
        this.color = color;
        this.appliedStyles = appliedStyles;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getText() {
        return displayText;
    }

    @Override
    public String getAltText() {
        return url;
    }

    @Override
    public EnumSet<Style> getStyles() {
        return ComponentUtils.copyStyleSet(appliedStyles);
    }

    @Override
    public SplittableInternalMessageComponent split(int index) {
        return new UrlComponent(url, displayText.substring(index), color, appliedStyles);
    }

    @Override
    public SplittableInternalMessageComponent split(int startIndex, int endIndex) {
        return new UrlComponent(url, displayText.substring(startIndex, endIndex), color, appliedStyles);
    }

    public boolean equals(Object other) {
        if(!(other instanceof UrlComponent)) {
            return false;
        }

        UrlComponent otherComponent = (UrlComponent) other;

        return this == otherComponent || (
                this.url.equals(otherComponent.url)
                && this.displayText.equals(otherComponent.displayText)
                && (this.color == null && otherComponent.color == null || this.color.equals(otherComponent.color))
                && this.appliedStyles.equals(otherComponent.appliedStyles)
        );
    }

    @Override
    public String toString() {
        return "UrlComponent{url='%s',displayText='%s',color=%s,styles=%s}".formatted(url, displayText, color, appliedStyles);
    }
}
