package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Message containing details of server performance.
 */
public class ServerStatusMessage extends InternalMessage {

    /**
     * Milliseconds per tick (MSPT).
     */
    private final double mspt;

    /**
     * Total memory reported by the JVM.
     */
    private final long totalMemoryBytes;

    /**
     * Free memory reported by the JVM.
     */
    private final long freeMemoryBytes;

    /**
     * Number of players connected to the server.
     */
    private final int playersOnline;

    /**
     * Maximum number of players the server is configured to allow.
     */
    private final int maxPlayers;

    /**
     * Server Message of the Day (MOTD).
     */
    private final List<TextComponent> motd;

    private final long timestamp;

    /**
     * Mapping of placeholder names to suppliers of the components to replace them.
     */
    private final Map<String, Supplier<List<? extends InternalMessageComponent>>> placeholderSuppliers = Map.of(
            "mspt",          () -> List.of(this.statToComponent(this.getMspt())),
            "tps",           () -> List.of(this.statToComponent(this.getTps())),
            "freeMem",       () -> List.of(this.bytesToComponent(this.getFreeMemoryBytes())),
            "usedMem",       () -> List.of(this.bytesToComponent(this.getUsedMemoryBytes())),
            "totalMem",      () -> List.of(this.bytesToComponent(this.getTotalMemoryBytes())),
            "playersOnline", () -> List.of(new TextComponent(String.valueOf(this.getPlayersOnline()))),
            "maxPlayers",    () -> List.of(new TextComponent(String.valueOf(this.getMaxPlayers()))),
            "motd",          this::getMotd
    );

    /**
     * Creates a new ServerStatusMessage as of the current time.
     * @param sourceId         The source ID of the system that generate this message (Minecraft).
     * @param mspt             Current milliseconds per tick (MSPT).
     * @param totalMemoryBytes Current total memory in bytes.
     * @param freeMemoryBytes  Current free memory in bytes.
     * @param playersOnline    Number of players currently connected.
     * @param maxPlayers       Maximum number of player connections the server will allow.
     * @param motd             Server Message of the Day (MOTD).
     */
    public ServerStatusMessage(String sourceId, double mspt, long totalMemoryBytes, long freeMemoryBytes, int playersOnline, int maxPlayers, List<TextComponent> motd) {
        this(sourceId, mspt, totalMemoryBytes, freeMemoryBytes, playersOnline, maxPlayers, motd, System.currentTimeMillis());
    }

    /**
     * Creates a new ServerStatusMessage as of the specified time.
     * @param sourceId         The source ID of the system that generate this message (Minecraft).
     * @param mspt             Current milliseconds per tick (MSPT).
     * @param totalMemoryBytes Current total memory in bytes.
     * @param freeMemoryBytes  Current free memory in bytes.
     * @param playersOnline    Number of players currently connected.
     * @param maxPlayers       Maximum number of player connections the server will allow.
     * @param motd             Server Message of the Day (MOTD).
     * @param timestamp        Unix epoch (milliseconds) representing the time this status was current.
     */
    public ServerStatusMessage(String sourceId, double mspt, long totalMemoryBytes, long freeMemoryBytes, int playersOnline, int maxPlayers, List<TextComponent> motd, long timestamp) {
        super(sourceId, "[Server status message]");
        this.mspt = mspt;
        this.totalMemoryBytes = totalMemoryBytes;
        this.freeMemoryBytes = freeMemoryBytes;
        this.playersOnline = playersOnline;
        this.maxPlayers = maxPlayers;
        this.motd = motd;
        this.timestamp = timestamp;
    }

    @Override
    protected List<? extends InternalMessageComponent> getComponentsForPlaceholder(String placeholder) {
        List<? extends InternalMessageComponent> components;
        if((components = super.getComponentsForPlaceholder(placeholder)) != null) {
            return components;
        }

        if(placeholderSuppliers.containsKey(placeholder)) {
            return placeholderSuppliers.get(placeholder).get();
        }

        return null;
    }

    /**
     * Gets the average milliseconds per tick (MSPT).
     * @return
     */
    public double getMspt() {
        return mspt;
    }

    /**
     * Gets the average ticks per second (TPS).
     * @return
     */
    public double getTps() {
        return Math.min(1000D / getMspt(), 20.0D);
    }

    /**
     * Gets the free memory reported by the JVM.
     * @return
     */
    public long getFreeMemoryBytes() {
        return freeMemoryBytes;
    }

    /**
     * Gets the total memory, used and free, reported by the JVM.
     * @return
     */
    public long getTotalMemoryBytes() {
        return totalMemoryBytes;
    }

    /**
     * Gets the used memory reported by the JVM.
     * @return
     */
    public long getUsedMemoryBytes() {
        return getTotalMemoryBytes() - getFreeMemoryBytes();
    }

    /**
     * Gets the number of players connected to the server.
     * @return
     */
    public int getPlayersOnline() {
        return playersOnline;
    }

    /**
     * Gets the maximum number of players the server is configured to allow.
     * @return
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Gets the server Message of the Day (MOTD).
     */
    public List<TextComponent> getMotd() {
        return motd;
    }

    /**
     * Gets the Unix epoch (in milliseconds) of the time this status was current.
     * @return
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Status update: %d/%d players online, MSPT: %.1f, TPS: %.1f, Mem Used: %dK Free: %dK Total: %dK"
                .formatted(
                        getPlayersOnline(),
                        getMaxPlayers(),
                        getMspt(),
                        getTps(),
                        getUsedMemoryBytes() / 1024,
                        getFreeMemoryBytes() / 1024,
                        getTotalMemoryBytes() / 1024
                );
    }

    /**
     * Formats a double stat in an appropriate format.
     * @param value The value to format.
     * @return TextComponent with formatted representation.
     */
    private TextComponent statToComponent(double value) {
        return new TextComponent("%.1f".formatted(value));
    }

    /**
     * Formats a storage amount in bytes in an appropriate format
     * (converts to KiB, MiB, GiB, TiB as necessary)
     * @param value
     * @return
     */
    private TextComponent bytesToComponent(long value) {
        if(value < 2L * 1024) {
            // if value is less than 2KiB, display in bytes
            return new TextComponent("%dB".formatted(value));
        }
        else if(value < 2L * 1024 * 1024) {
            // if value is less than 2MiB, display in KiB
            return new TextComponent("%.2fKiB".formatted(value / 1024.0D));
        }
        else if(value < 2L * 1024 * 1024 * 1024) {
            // if value is less than 2GiB, display in MiB
            return new TextComponent("%.2fMiB".formatted(value / 1024.0D / 1024.0D));
        }
        else if(value < 2L * 1024 * 1024 * 1024 * 1024) {
            // if value is less than 2TiB, display in GiB
            return new TextComponent("%.2fGiB".formatted(value / 1024.0D / 1024.0D / 1024.0D));
        }
        else {
            // display in TiB
            // if you have more than 2PiB of RAM, who are you
            // and why do you care about Minecraft?
            // seriously, please open an issue wherever this code
            // is hosted and tell me. i will be happy to extend
            // this method just for you. -0x
            return new TextComponent("%.2fTiB".formatted(value / 1024.0D / 1024.0D / 1024.0D / 1024.0D));
        }
    }
}
