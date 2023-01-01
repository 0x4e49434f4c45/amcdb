package network.parthenon.amcdb.minecraft;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.message.ConsoleMessage;
import network.parthenon.amcdb.messaging.BackgroundMessageBroker;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.io.File;

public class LogTailer implements TailerListener {

    private static int threadNum = 1;

    private Tailer tailer;

    private LogTailer() {}

    @Override
    public void init(Tailer tailer) {
        this.tailer = tailer;
    }

    @Override
    public void fileNotFound() {
        AMCDB.LOGGER.warn("Minecraft log file '" + tailer.getFile().toString() + "' was not found!");
    }

    @Override
    public void fileRotated() {
        AMCDB.LOGGER.info("New log file detected.");
    }

    @Override
    public void handle(String line) {
        BackgroundMessageBroker.publish(new ConsoleMessage(MinecraftService.MINECRAFT_SOURCE_ID, line));
    }

    @Override
    public void handle(Exception ex) {
        AMCDB.LOGGER.error("Minecraft log tailer encountered an error", ex);
    }

    /**
     * Configures a LogTailer to watch the specified file on a new thread.
     *
     * @param file The file to watch.
     */
    public static void watchFile(File file) {
        TailerListener listener = new LogTailer();
        Tailer tailer = new Tailer(file, listener);
        Thread tailerThread = new Thread(tailer);
        tailerThread.setDaemon(true);
        tailerThread.setName("amcdb-tail-%s-%d".formatted(file.getName(), threadNum++));
        tailerThread.start();
    }
}
