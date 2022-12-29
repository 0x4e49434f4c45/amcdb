package network.parthenon.amcdb.messaging;

/**
 * Receives InternalMessages published to the message broker.
 */
public interface MessageHandler {

    /**
     * Called when a new message is published.
     * @param message The message that was published.
     */
    public void handleMessage(InternalMessage message);

    /**
     * Gets the source ID of the publisher corresponding to this handler, if any.
     *
     * If this method returns a non-null, non-empty string, the message broker will
     * skip sending messages to this handler if their source ID matches the returned
     * value (i.e. handleMessage() will not be called).
     *
     * @return Source ID to ignore.
     */
    public String getOwnSourceId();
}
