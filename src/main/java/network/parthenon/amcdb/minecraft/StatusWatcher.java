package network.parthenon.amcdb.minecraft;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.messaging.message.ServerStatusMessage;
import network.parthenon.amcdb.util.IntervalRunnable;

import java.util.List;

public class StatusWatcher extends IntervalRunnable {

    private final MinecraftService minecraftService;

    private final BackgroundMessageBroker broker;

    public StatusWatcher(MinecraftService minecraftService, BackgroundMessageBroker broker) {
        super("AMCDB Server Status Watcher");
        this.minecraftService = minecraftService;
        this.broker = broker;
    }

    @Override
    public void run() {
        double mspt = getAverageMspt();

        broker.publish(new ServerStatusMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                mspt,
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory(),
                minecraftService.getMinecraftServerInstance().getCurrentPlayerCount(),
                minecraftService.getMinecraftServerInstance().getMaxPlayerCount(),
                List.of(new TextComponent(minecraftService.getMinecraftServerInstance().getServerMotd()))
        ));
    }

    private double getAverageMspt() {
        long[] tickLengths = minecraftService.getMinecraftServerInstance().lastTickLengths;
        long totalTickTime = 0;

        for(long tickLength : tickLengths) {
            totalTickTime += tickLength;
        }

        return (double)totalTickTime / tickLengths.length * 1.0E-6D;
    }
}
