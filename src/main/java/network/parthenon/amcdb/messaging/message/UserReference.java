package network.parthenon.amcdb.messaging.message;

import java.awt.*;
import java.util.EnumSet;
import java.util.Set;

/**
 * Represents a user of a target system (i.e. Minecraft or Discord).
 *
 *
 */
public class UserReference implements InternalMessageComponent {

    private String userId;

    private String displayName;

    private Color color;

    private EnumSet<Style> appliedStyles;

    public UserReference(String userId) {
        this(userId, userId, null, EnumSet.noneOf(Style.class));
    }

    public UserReference(String userId, String displayName) {
        this(userId, displayName, null, EnumSet.noneOf(Style.class));
    }

    public UserReference(String userId, String displayName, Color color) {
        this(userId, displayName, color, EnumSet.noneOf(Style.class));
    }

    public UserReference(String userId, String displayName, Color color, EnumSet<Style> appliedStyles) {
        if(userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if(displayName == null) {
            throw new IllegalArgumentException("displayName must not be null");
        }

        this.userId = userId;
        this.displayName = displayName;
        this.color = color;
        this.appliedStyles = appliedStyles;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getText() {
        return "@" + this.displayName;
    }

    @Override
    public Set<Style> getStyles() {
        return this.appliedStyles;
    }
}
