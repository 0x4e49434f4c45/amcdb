package network.parthenon.amcdb.messaging;

import network.parthenon.amcdb.messaging.message.InternalMessage;

public interface MessageBroker {
    /**
     * Subscribes a handler to messages.
     *
     * @param handler The handler to subscribe.
     */
    void subscribe(MessageHandler handler);

    /**
     * Publishes message(s) to the queue and returns immediately.
     * Handlers are invoked on separate threads.
     * <p>
     * This method is synchronized so that if multiple messages are
     * supplied in a single call, they are guaranteed to be published
     * sequentially with no gaps.
     *
     * @param messages The message(s) to publish.
     */
    void publish(InternalMessage... messages);
}
