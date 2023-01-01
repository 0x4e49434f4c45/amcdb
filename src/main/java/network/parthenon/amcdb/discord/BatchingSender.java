package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import network.parthenon.amcdb.util.IntervalRunnable;

import java.util.concurrent.*;

/**
 * Sends messages to a Discord channel in batches of up to {@link DiscordService#DISCORD_MESSAGE_CHAR_LIMIT} characters.
 */
class BatchingSender extends IntervalRunnable {

    /**
     * JDA TextChannel on which messages will be sent.
     */
    private TextChannel channel;

    /**
     * Queue of messages ready to be sent. Messages on this queue must be
     * less than {@link DiscordService#DISCORD_MESSAGE_CHAR_LIMIT} characters.
     */
    private LinkedTransferQueue<String> messageQueue;

    /**
     * Creates a BatchingSender for the specified channel.
     * @param channel JDA TextChannel on which messages will be sent.
     */
    public BatchingSender(TextChannel channel) {
        super("discord-%s-sender".formatted(channel.getName()));
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
            StringBuilder messageBuilder = new StringBuilder(DiscordService.DISCORD_MESSAGE_CHAR_LIMIT);

            while(true) {
                String message = messageQueue.peek();
                if(message == null) {
                    // reached end of queue
                    break;
                }

                boolean firstMessage = messageBuilder.length() == 0;
                // check whether we can fit the next message into the batch
                if(!firstMessage && messageBuilder.length() + message.length() + 1 > DiscordService.DISCORD_MESSAGE_CHAR_LIMIT) {
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
        messageQueue.add(message);
    }
}
