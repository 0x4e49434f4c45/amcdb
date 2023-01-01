package network.parthenon.amcdb.messaging.component;

import java.util.Collection;
import java.util.EnumSet;

public class ComponentUtils {

    /**
     * Copies the provided styles to a new EnumSet.
     * @param styles The collection of styles to copy.
     * @return New EnumSet.
     */
    public static EnumSet<InternalMessageComponent.Style> copyStyleSet(
            Collection<InternalMessageComponent.Style> styles) {
        return styles.isEmpty() ?
                EnumSet.noneOf(InternalMessageComponent.Style.class) :
                EnumSet.copyOf(styles);
    }

    /**
     * Toggles the specified style in the provided set.
     * @param style The style to toggle.
     * @param set The set to update.
     */
    public static void toggleStyle(InternalMessageComponent.Style style, EnumSet<InternalMessageComponent.Style> set) {
        if(set.contains(style)) {
            set.remove(style);
        }
        else {
            set.add(style);
        }
    }
}
