package network.parthenon.amcdb.messaging.message;

public interface SplittableInternalMessageComponent extends InternalMessageComponent {

    /**
     * Returns a new SplittableInternalMessageComponent with the same style as this one
     * and content beginning at the specified index.
     * @param index The index at which to begin the content.
     */
    public SplittableInternalMessageComponent split(int index);
}
