package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import network.parthenon.amcdb.AMCDB;
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
     * Whether to place messages on the JDA queue when the connection is unavailable.
     */
    private boolean queueIfUnavailable;

    /**
     * Whether a notification of skipped messages should be sent when the JDA
     * connection resumes.
     */
    private boolean jdaDisconnected = false;

    /**
     * Creates a BatchingSender for the specified channel.
     * @param channel JDA TextChannel on which messages will be sent.
     * @param queueIfUnavailable Whether to place messages on the JDA queue
     *                           when the connection is unavailable.
     */
    public BatchingSender(TextChannel channel, boolean queueIfUnavailable) {
        super("discord-%s-sender".formatted(channel.getName()));
        this.channel = channel;
        this.queueIfUnavailable = queueIfUnavailable;
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

            if(!channel.getJDA().getStatus().isInit()) {
                // post a notification when the connection returns that messages have been skipped
                jdaDisconnected = true;
                if(!queueIfUnavailable) {
                    // if JDA says the connection is unavailable and we're configured to skip queueing, do so
                    // note that we don't evaluate jdaDisconnected here -- otherwise we will never retry!
                    // we only make a best effort to skip queueing messages if the connection is unavailable :/
                    return;
                }
            }
            channel.sendMessage(messageBuilder).queue((m) -> {
                // connection was previously disconnected; post a message
                if(jdaDisconnected) {
                    this.enqueueMessage("Discord connection was lost and has been restored. Messages may have been skipped; check logs.");
                }
                // we have a successful post, mark the connection good
                jdaDisconnected = false;
            }, (e) -> {
                if(!jdaDisconnected) {
                    // only log a message if the connection hasn't already been marked broken
                    // this stops redundant log messages from causing further JDA errors
                    AMCDB.LOGGER.error("Failed to send message to Discord!", e);
                }
                // we've received an error, mark the connection bad
                jdaDisconnected = true;
            });
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
