package network.parthenon.amcdb.messaging;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Message broker that dispatches messages to handlers using a background thread
 * to prevent blocking other threads.
 */
public class BackgroundMessageBroker {

    private static final String THREAD_POOL_PREFIX = "amcdb-dispatcher-pool-";

    private static BackgroundMessageBroker instance = new BackgroundMessageBroker();

    private Set<MessageHandler> handlers;

    private ExecutorService handlerPool;

    private BackgroundMessageBroker() {
        this.handlers = new HashSet<>();

        this.handlerPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private ThreadFactory defaultFactory = Executors.defaultThreadFactory();
            @Override
            public Thread newThread(@NotNull Runnable runnable) {
                Thread thread = defaultFactory.newThread(runnable);

                // set thread name to something useful
                String oldName = thread.getName();
                int dashIndex;
                // steal the old thread number rather than try to generate one
                if((dashIndex = oldName.lastIndexOf("-")) != -1) {
                    thread.setName(THREAD_POOL_PREFIX + oldName.substring(dashIndex + 1));
                }

                return thread;
            }
        });
    }

    /**
     * Subscribes a handler to messages.
     * @param handler The handler to subscribe.
     */
    public static void subscribe(MessageHandler handler) {
        instance.handlers.add(handler);
    }

    /**
     * Publishes a message to the queue and returns immediately.
     * Handlers are invoked on separate threads.
     * @param message The message to publish.
     */
    public static void publish(InternalMessage message) {
        instance.dispatchToHandlers(message);
    }

    /**
     * Dispatches a message to handlers using the thread pool.
     *
     * Skips handler(s) for the same source that published the message.
     *
     * @param message The message to dispatch.
     */
    private void dispatchToHandlers(InternalMessage message) {
        for(MessageHandler handler : handlers) {
            // Skip the handler for the source that published this message.
            if(message.getSourceId() != null
                    && message.getSourceId().length() > 0
                    && handler.getOwnSourceId() != null
                    && handler.getOwnSourceId().length() > 0
                    && message.getSourceId().equals(handler.getOwnSourceId())
            ) {
                continue;
            }

            // Run the message handler on the thread pool.
            handlerPool.submit(() -> {
                handler.handleMessage(message);
            });
        }
    }
}
