package network.parthenon.amcdb.messaging.component;

import java.awt.*;
import java.util.EnumSet;

/**
 * Represents a user of a target system (i.e. Minecraft or Discord).
 *
 *
 */
public class EntityReference implements InternalMessageComponent {

    private final String entityId;

    private final String displayName;

    private final String alternateName;

    private final Color color;

    private final EnumSet<Style> appliedStyles;

    public EntityReference(String entityId) {
        this(entityId, entityId, null,null, EnumSet.noneOf(Style.class));
    }

    public EntityReference(String entityId, String displayName) {
        this(entityId, displayName, null, null, EnumSet.noneOf(Style.class));
    }

    public EntityReference(String entityId, String displayName, String alternateName) {
        this(entityId, displayName, alternateName, null, EnumSet.noneOf(Style.class));
    }

    public EntityReference(String entityId, String displayName, String alternateName, Color color) {
        this(entityId, displayName, alternateName, color, EnumSet.noneOf(Style.class));
    }

    public EntityReference(String entityId, String displayName, String alternateName, Color color, EnumSet<Style> appliedStyles) {
        if(entityId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if(displayName == null) {
            throw new IllegalArgumentException("displayName must not be null");
        }

        this.entityId = entityId;
        this.displayName = displayName;
        this.alternateName = alternateName;
        this.color = color;
        this.appliedStyles = appliedStyles;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAlternateName() { return alternateName; }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getText() {
        return displayName;
    }

    @Override
    public String getAltText() {
        return alternateName;
    }

    @Override
    public EnumSet<Style> getStyles() {
        return ComponentUtils.copyStyleSet(appliedStyles);
    }
}
