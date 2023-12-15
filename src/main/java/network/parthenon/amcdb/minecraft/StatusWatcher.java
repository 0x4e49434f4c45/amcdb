package network.parthenon.amcdb.minecraft;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.MessageBroker;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.messaging.message.ServerStatusMessage;
import network.parthenon.amcdb.util.IntervalRunnable;

import java.util.List;

public class StatusWatcher extends IntervalRunnable {

    private final MinecraftService minecraftService;

    private final MessageBroker broker;

    public StatusWatcher(MinecraftService minecraftService, MessageBroker broker) {
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
        //#if MC>=12003
        long[] tickLengths = minecraftService.getMinecraftServerInstance().getTickTimes();
        //#else
        //$$ long[] tickLengths = minecraftService.getMinecraftServerInstance().lastTickLengths;
        //#endif
        long totalTickTime = 0;

        for(long tickLength : tickLengths) {
            totalTickTime += tickLength;
        }

        return (double)totalTickTime / tickLengths.length * 1.0E-6D;
    }
}
