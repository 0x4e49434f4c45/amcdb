package network.parthenon.amcdb.messaging.component;

import java.awt.*;
import java.util.EnumSet;

/**
 * Represents a user of a target system (i.e. Minecraft or Discord).
 */
public class EntityReference implements InternalMessageComponent {

    private final String entityId;

    private final String displayName;

    private final String alternateName;

    private final String imageUrl;

    private final Color color;

    private final EnumSet<Style> appliedStyles;

    public EntityReference(String entityId) {
        this(entityId, entityId, entityId, null, EnumSet.noneOf(Style.class), null);
    }

    public EntityReference(String entityId, String displayName) {
        this(entityId, displayName, displayName, null, EnumSet.noneOf(Style.class), null);
    }

    public EntityReference(String entityId, String displayName, String alternateName) {
        this(entityId, displayName, alternateName, null, EnumSet.noneOf(Style.class), null);
    }

    public EntityReference(String entityId, String displayName, String alternateName, Color color) {
        this(entityId, displayName, alternateName, color, EnumSet.noneOf(Style.class), null);
    }

    public EntityReference(String entityId, String displayName, String alternateName, Color color, EnumSet<Style> appliedStyles) {
        this(entityId, displayName, alternateName, color, appliedStyles, null);
    }

    public EntityReference(
            String entityId,
            String displayName,
            String alternateName,
            Color color,
            EnumSet<Style> appliedStyles,
            String imageUrl) {
        if(entityId == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
        if(displayName == null) {
            throw new IllegalArgumentException("displayName must not be null");
        }

        this.entityId = entityId;
        this.displayName = displayName;
        this.alternateName = alternateName;
        this.color = color;
        this.appliedStyles = appliedStyles;
        this.imageUrl = imageUrl;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAlternateName() { return alternateName; }

    public String getImageUrl() { return imageUrl; }

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

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof EntityReference)) {
            return false;
        }

        EntityReference otherComponent = (EntityReference) other;

        return this == otherComponent || (
                this.entityId.equals(otherComponent.entityId)
                && this.displayName.equals(otherComponent.displayName)
                && (this.alternateName == null && otherComponent.alternateName == null || this.alternateName.equals(otherComponent.alternateName))
                && (this.color == null && otherComponent.color == null || this.color.equals(otherComponent.color))
                && this.appliedStyles.equals(otherComponent.appliedStyles)
        );
    }

    @Override
    public String toString() {
        return "EntityReference{entityId='%s',displayName='%s',alternateName=%s,color=%s,styles=%s}"
                .formatted(entityId, displayName, alternateName, color, appliedStyles);
    }
}
