package network.parthenon.amcdb.minecraft;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.messaging.message.ServerStatusMessage;
import network.parthenon.amcdb.util.IntervalRunnable;

import java.util.List;

public class StatusWatcher extends IntervalRunnable {
    @Override
    public void run() {
        double mspt = getAverageMspt();

        BackgroundMessageBroker.getInstance().publish(new ServerStatusMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                mspt,
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory(),
                AMCDB.getMinecraftServerInstance().getCurrentPlayerCount(),
                AMCDB.getMinecraftServerInstance().getMaxPlayerCount(),
                List.of(new TextComponent(AMCDB.getMinecraftServerInstance().getServerMotd()))
        ));
    }

    public StatusWatcher() {
        super("AMCDB Server Status Watcher");
    }

    private static double getAverageMspt() {
        long[] tickLengths = AMCDB.getMinecraftServerInstance().lastTickLengths;
        long totalTickTime = 0;

        for(long tickLength : tickLengths) {
            totalTickTime += tickLength;
        }

        return (double)totalTickTime / tickLengths.length * 1.0E-6D;
    }
}
