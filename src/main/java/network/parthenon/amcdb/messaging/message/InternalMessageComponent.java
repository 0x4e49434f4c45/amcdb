package network.parthenon.amcdb.messaging.message;

import java.awt.*;
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

    public Set<Style> getStyles();
}
