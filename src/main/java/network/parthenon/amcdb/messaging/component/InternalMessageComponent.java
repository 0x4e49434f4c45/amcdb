package network.parthenon.amcdb.messaging.component;

import java.awt.*;
import java.util.EnumSet;
import java.util.Set;

public interface InternalMessageComponent {

    /**
     * Styles that can be applied to a TextComponent.
     *
     * If a system does not support a particular Style, it will be ignored.
     */
    public enum Style { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, OBFUSCATED }

    public Color getColor();

    public String getText();

    public String getAltText();

    public EnumSet<Style> getStyles();
}
