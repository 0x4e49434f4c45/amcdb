package network.parthenon.amcdb.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import network.parthenon.amcdb.AMCDB;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebhookSender {
    private final String url;

    private final OkHttpClient httpClient;

    public WebhookSender(String url) {
        this.url = url;
        this.httpClient = new OkHttpClient();
    }

    public void send(String message, String username, String avatarUrl) {
        ObjectNode jsonObject = new ObjectMapper().createObjectNode();
        jsonObject.put("content", message);
        jsonObject.put("username", username);
        jsonObject.put("avatar_url", avatarUrl);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonObject.toString(), MediaType.get("application/json")))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(!response.isSuccessful()) {
                    AMCDB.LOGGER.error(
                            "Failed to send message via Discord webhook: (%d) %s"
                                    .formatted(response.code(), response.body().string())
                    );
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                AMCDB.LOGGER.error("Failed to send message via Discord webhook", e);
            }
        });
    }
}
