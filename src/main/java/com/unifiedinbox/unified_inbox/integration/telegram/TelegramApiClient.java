
package com.unifiedinbox.unified_inbox.integration.telegram;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class TelegramApiClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;

    public TelegramApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TelegramBotInfo getMe(String token) {

        if (token == null ||
                !token.matches("[0-9]+:[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Invalid Telegram bot token."
            );
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.telegram.org/bot"
                                    + token + "/getMe"
                    ))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 401 ||
                    response.statusCode() == 404) {
                throw new IllegalArgumentException(
                        "Invalid Telegram bot token."
                );
            }

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Telegram API is temporarily unavailable."
                );
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("ok").asBoolean(false)) {
                throw new IllegalArgumentException(
                        "Telegram could not verify this bot."
                );
            }

            JsonNode bot = root.path("result");

            if (bot.isMissingNode() || bot.isNull()) {
                throw new IllegalArgumentException(
                        "Telegram returned no bot information."
                );
            }

            return new TelegramBotInfo(
                    bot.path("id").asLong(),
                    bot.path("username").asString(),
                    bot.path("first_name").asString()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Telegram request was interrupted.",
                    exception
            );

        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Could not connect to Telegram API.",
                    exception
            );
        }

        
    }


    public void setWebhook(
            String token,
            String webhookUrl,
            String secret
    ) {
    try {
            String json = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "url", webhookUrl,
                            "secret_token", secret,
                            "allowed_updates",
                            java.util.List.of("message")
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.telegram.org/bot"
                                    + token + "/setWebhook"
                    ))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode result = objectMapper.readTree(
                    response.body()
            );

            if (response.statusCode() != 200
                    || !result.path("ok").asBoolean(false)) {

            throw new IllegalStateException(
                    "Failed to register Telegram webhook: HTTP "
                            + response.statusCode()
            );
            }

    } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Telegram request was interrupted",
                    exception
            );

    } catch (java.io.IOException exception) {

            throw new IllegalStateException(
                    "Could not connect to Telegram API",
                    exception
            );
    }
    }

    
    public String sendMessage(
            String token,
            String chatId,
            String text
    ) {
        try {
            String json = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "chat_id", chatId,
                            "text", text
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.telegram.org/bot"
                                    + token + "/sendMessage"
                    ))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() != 200
                    || !root.path("ok").asBoolean(false)) {
                throw new IllegalStateException(
                        "Telegram failed to send message: HTTP "
                                + response.statusCode()
                );
            }

            return root.path("result")
                    .path("message_id")
                    .asText();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Telegram request was interrupted",
                    exception
            );
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Could not connect to Telegram API",
                    exception
            );
        }
    }


    public record TelegramBotInfo(
            Long id,
            String username,
            String first_name
    ) {}
}
