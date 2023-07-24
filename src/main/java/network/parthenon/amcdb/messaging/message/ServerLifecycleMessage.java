package network.parthenon.amcdb.messaging.message;

/**
 * Message notifying of a server lifecyle event (started, stopped, etc).
 */
public class ServerLifecycleMessage extends InternalMessage {

    private Event event;

    private ServerLifecycleMessage(String sourceId, String text, Event event) {
        super(sourceId, text);
        this.event = event;
    }

    /**
     * Gets the event for this lifecycle message.
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Creates a lifecycle message indicating that the server has started.
     */
    public static ServerLifecycleMessage started(String sourceId) {
        return new ServerLifecycleMessage(sourceId, "Server started", Event.STARTED);
    }

    /**
     * Creates a lifecycle message indicating that the server has stopped.
     */
    public static ServerLifecycleMessage stopped(String sourceId) {
        return new ServerLifecycleMessage(sourceId, "Server stopped", Event.STOPPED);
    }

    /**
     * Lifecycle event types.
     */
    public static enum Event {
        STARTED,
        STOPPED
    }
}
