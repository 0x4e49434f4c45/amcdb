package network.parthenon.amcdb.minecraft;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import network.parthenon.amcdb.config.AMCDBConfig;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinecraftService {

    private static MinecraftService instance;

    public static final String MINECRAFT_SOURCE_ID = "Minecraft";

    private static final String LOG_FILE = AMCDBConfig.getRequiredProperty("amcdb.minecraft.logFile");

    private final ConcurrentMap<String, Integer> recentlyPublishedContents;

    private MinecraftService() {
        recentlyPublishedContents = new ConcurrentHashMap<>();
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

    /**
     * Registers various event handlers for MinecraftService.
     *
     * Called at mod initialization.
     */
    public static void init() {
        // Subscribe to in game messages
        ServerMessageEvents.CHAT_MESSAGE.register(InGameMessageHandler::handleChatMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(InGameMessageHandler::handleCommandMessage);
        ServerMessageEvents.GAME_MESSAGE.register(InGameMessageHandler::handleGameMessage);

        // Subscribe to message broker
        BackgroundMessageBroker.subscribe(new MinecraftPublisher());

        // Defer reading log file until mods are fully loaded
        // This will ensure that all message handlers are ready
        ServerLifecycleEvents.SERVER_STARTING.register(e -> {
            // Subscribe to console logs
            LogTailer.watchFile(new File(LOG_FILE));
        });
    }

    public static void shutdown() {
        // Do nothing
    }

    /**
     * Gets the MinecraftService instance.
     */
    public static MinecraftService getInstance() {
        return instance == null ?
                instance = new MinecraftService() :
                instance;
    }
}
