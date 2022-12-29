package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.*;

/**
 * Sends messages to a Discord channel in batches of up to 2,000 characters.
 */
class BatchingSender implements Runnable {

    private static final int DISCORD_MESSAGE_CHAR_LIMIT = 2000;

    private TextChannel channel;

    private LinkedTransferQueue<String> messageQueue;

    private ScheduledExecutorService executorService;

    public BatchingSender(TextChannel channel) {
        this.channel = channel;
        messageQueue = new LinkedTransferQueue<>();
    }

    @Override
    public void run() {
        while(!messageQueue.isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder(DISCORD_MESSAGE_CHAR_LIMIT);

            while(true) {
                String message = messageQueue.peek();
                if(message == null) {
                    // reached end of queue
                    break;
                }

                boolean firstMessage = messageBuilder.length() == 0;
                // check whether we can fit the next message into the batch
                if(!firstMessage && messageBuilder.length() + message.length() + 1 > DISCORD_MESSAGE_CHAR_LIMIT) {
                    break;
                }

                if(!firstMessage) {
                    messageBuilder.append("\n");
                }
                messageBuilder.append(messageQueue.poll());
            }

            channel.sendMessage(messageBuilder).queue();
        }
    }

    /**
     * Enqueues the provided message to be sent with the next batch.
     * Will split messages over the maximum length into multiple batches.
     *
     * @param message The message to send.
     */
    public void enqueueMessage(String message) {
        int index = 0;

        while(message.length() - index > 0) {
            messageQueue.add(message.substring(index, Math.min(message.length(), index + DISCORD_MESSAGE_CHAR_LIMIT)));
            index += DISCORD_MESSAGE_CHAR_LIMIT;
        }
    }

    public ExecutorService start(long intervalMillis) {
        if(executorService != null) {
            throw new IllegalStateException("BatchingSender is already started! (channel: %s)".formatted(channel.getName()));
        }

        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("discord-%s-sender".formatted(channel.getName()));
            return thread;
        });

        executorService.scheduleWithFixedDelay(this, 0, intervalMillis, TimeUnit.MILLISECONDS);

        return executorService;
    }
}
