package network.parthenon.amcdb.messaging;

import java.awt.*;

/**
 * Represents a user of a target system (i.e. Minecraft or Discord).
 *
 *
 */
public class UserReference implements InternalMessageComponent {

    private String userId;

    private String displayName;

    private Color color;

    public UserReference(String userId) {
        this(userId, userId, null);
    }

    public UserReference(String userId, String displayName) {
        this(userId, displayName, null);
    }

    public UserReference(String userId, String displayName, Color color) {
        if(userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if(displayName == null) {
            throw new IllegalArgumentException("displayName must not be null");
        }

        this.userId = userId;
        this.displayName = displayName;
        this.color = color;
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
}
