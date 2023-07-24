package network.parthenon.amcdb.minecraft;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import network.parthenon.amcdb.config.AMCDBPropertiesConfig;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.messaging.message.ServerLifecycleMessage;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinecraftService {

    public static final String MINECRAFT_SOURCE_ID = "Minecraft";

    private final MinecraftConfig config;

    private final MessageBroker broker;

    private final ConcurrentMap<String, Integer> recentlyPublishedContents;

    private MinecraftServer minecraftServerInstance;

    /**
     * Creates and initializes the MinecraftService.
     * @param broker
     * @param config
     */
    public MinecraftService(MessageBroker broker, MinecraftConfig config) {
        this.config = config;
        this.broker = broker;
        recentlyPublishedContents = new ConcurrentHashMap<>();

        InGameMessageHandler handler = new InGameMessageHandler(this, config, broker);
        // Subscribe to in game messages
        ServerMessageEvents.CHAT_MESSAGE.register(handler::handleChatMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(handler::handleCommandMessage);
        ServerMessageEvents.GAME_MESSAGE.register(handler::handleGameMessage);

        // Subscribe to message broker
        broker.subscribe(new MinecraftPublisher(this, config));

        // Defer reading log file until mods are fully loaded
        // This will ensure that all message handlers are ready
        ServerLifecycleEvents.SERVER_STARTING.register(e -> {
            // Subscribe to console logs
            LogTailer.watchFile(new File(config.getMinecraftLogFile()), broker);
        });

        // Defer starting status watcher until server is done loading
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            minecraftServerInstance = server;
            broker.publish(ServerLifecycleMessage.started(MINECRAFT_SOURCE_ID));
            new StatusWatcher(this, broker).start(10000);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            broker.publish(ServerLifecycleMessage.stopped(MINECRAFT_SOURCE_ID));
        });
    }

    /**
     * Registers a particular string as having been sent by AMCDB.
     *
     * Every message broadcast on the game chat is also picked up by the game chat
     * handler, which needs to check if the received message corresponds to one
     * published by AMCDB in order to avoid sending a duplicate message to the
     * internal message broker.
     *
     * @param contents The contents to register.
     */
    public void addRecentlyPublished(String contents) {
        recentlyPublishedContents.compute(contents, (k, v) -> v == null ? 1 : v + 1);
    }

    /**
     * Matches the given contents to a recently published message, if any is found.
     *
     * For a given string, this method will only return true once per instance that
     * message was published to the game chat by AMCDB.
     *
     * @param contents The contents to check.
     * @return Whether a matching message was found and consumed.
     */
    public boolean checkAndConsumeRecentlyPublished(String contents) {
        // optimistic non-locking check
        if(!recentlyPublishedContents.containsKey(contents)) {
            return false;
        }

        // thread-safely decrement the count and remove key if appropriate
        // (acquires lock)
        AtomicBoolean wasRecentlyPublished = new AtomicBoolean(false);
        recentlyPublishedContents.compute(contents, (k, v) -> {
            if(v == null) {
                return null;
            }
            wasRecentlyPublished.set(true);
            return v == 1 ? null : v - 1;
        });
        return wasRecentlyPublished.get();
    }

    public void shutdown() {
        // Do nothing
    }

    /**
     * Gets the MinecraftServer instance.
     *
     * CAUTION! May return null if the server is not yet initialized.
     * @return MinecraftServer instance, or null if the server is not initialized.
     */
    public MinecraftServer getMinecraftServerInstance() {
        return minecraftServerInstance;
    }
}
