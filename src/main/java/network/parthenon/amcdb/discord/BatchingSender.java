package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.*;

/**
 * Sends messages to a Discord channel in batches of up to {@link #DISCORD_MESSAGE_CHAR_LIMIT} characters.
 */
class BatchingSender implements Runnable {

    /**
     * Maximum number of characters allowed in a Discord message.
     */
    private static final int DISCORD_MESSAGE_CHAR_LIMIT = 2000;

    /**
     * JDA TextChannel on which messages will be sent.
     */
    private TextChannel channel;

    /**
     * Queue of messages ready to be sent. Messages on this queue must be
     * less than {@link #DISCORD_MESSAGE_CHAR_LIMIT} characters.
     */
    private LinkedTransferQueue<String> messageQueue;

    /**
     * Executor used to schedule message batching.
     */
    private ScheduledExecutorService executorService;

    /**
     * Creates a BatchingSender for the specified channel.
     * @param channel JDA TextChannel on which messages will be sent.
     */
    public BatchingSender(TextChannel channel) {
        this.channel = channel;
        messageQueue = new LinkedTransferQueue<>();
    }

    /**
     * Executes a batching round.
     *
     * Do not call this method. It is called by the executor.
     */
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
     * Splits messages over the maximum length as necessary.
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

    /**
     * Schedules batching and sending of queued messages at the specified interval.
     * @param intervalMillis Interval at which to check for messages
     * @return ScheduledExecutorService managing the schedule
     */
    public ScheduledExecutorService start(long intervalMillis) {
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
