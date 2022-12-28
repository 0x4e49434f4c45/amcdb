package network.parthenon.amcdb.messaging;

import io.netty.util.concurrent.DefaultThreadFactory;
import network.parthenon.amcdb.AMCDB;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Message broker that dispatches messages to handlers using a thread pool
 * to prevent blocking other threads.
 */
public class ThreadPoolMessageBroker {

    private static final String THREAD_POOL_PREFIX = "amcdb-dispatcher-pool-";

    private static ThreadPoolMessageBroker instance = new ThreadPoolMessageBroker();

    private Set<MessageHandler> handlers;

    private ExecutorService handlerPool;

    private ThreadPoolMessageBroker() {
        this.handlers = new HashSet<>();

        this.handlerPool = Executors.newCachedThreadPool(new ThreadFactory() {
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
        AMCDB.LOGGER.info("Message published: " + message.toString());

        instance.dispatchToHandlers(message);
    }

    /**
     * Dispatches a message to handlers using the thread pool.
     * @param message The message to dispatch.
     */
    private void dispatchToHandlers(InternalMessage message) {
        for(MessageHandler handler : handlers) {
            handlerPool.submit(() -> {
                handler.handleMessage(message);
            });
        }
    }
}
