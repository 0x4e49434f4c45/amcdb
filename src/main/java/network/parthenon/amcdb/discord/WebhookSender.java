package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IncomingWebhookClient;
import net.dv8tion.jda.api.entities.WebhookClient;
import network.parthenon.amcdb.AMCDB;

public class WebhookSender {
    private final IncomingWebhookClient webhook;

    public WebhookSender(JDA jda, String url) {
        webhook = WebhookClient.createClient(jda, url);
    }

    public void send(String message, String username, String avatarUrl) {
        webhook.sendMessage(message)
                .setUsername(username)
                .setAvatarUrl(avatarUrl)
                .queue(msg -> {}, e -> AMCDB.LOGGER.error("Failed to send message via Discord webhook", e));
    }
}
