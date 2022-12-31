package network.parthenon.amcdb.messaging.message;

public interface SplittableInternalMessageComponent extends InternalMessageComponent {

    /**
     * Returns a new SplittableInternalMessageComponent with the same style as this one
     * and content beginning at the specified index.
     * @param index The index at which to begin the content.
     */
    public SplittableInternalMessageComponent split(int index);

    /**
     * Returns a new SplittableInternalMessageComponent with the same style as this one
     * and content beginning at the specified startIndex and continuing up to (but not
     * including) the specified endIndex.
     * @param startIndex The index at which to begin the content.
     * @param endIndex The index before which to end the content.
     */
    public SplittableInternalMessageComponent split(int startIndex, int endIndex);
}
