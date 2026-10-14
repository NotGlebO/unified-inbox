
package com.unifiedinbox.unified_inbox.integration.telegram;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException;
import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException.FailureType;

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
        String json;

        try {
            json = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "chat_id", chatId,
                            "text", text
                    )
            );
        } catch (Exception exception) {
            throw new MessageSendException(
                    FailureType.PERMANENT,
                    "Could not serialize Telegram message.",
                    exception
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.telegram.org/bot"
                                + token + "/sendMessage"
                ))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new MessageSendException(
                    FailureType.UNKNOWN,
                    "Telegram request was interrupted.",
                    exception
            );
        } catch (java.io.IOException exception) {
            throw new MessageSendException(
                    FailureType.UNKNOWN,
                    "Telegram request failed; delivery is uncertain.",
                    exception
            );
        }

        int status = response.statusCode();

        if (status == 429) {
            throw new MessageSendException(
                    FailureType.RETRYABLE,
                    "Telegram rate limit exceeded."
            );
        }

        if (status == 400 || status == 401
                || status == 403 || status == 404) {
            throw new MessageSendException(
                    FailureType.PERMANENT,
                    "Telegram rejected the message: HTTP " + status
            );
        }

        if (status != 200) {
            throw new MessageSendException(
                    FailureType.UNKNOWN,
                    "Unexpected Telegram response: HTTP " + status
            );
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("ok").asBoolean(false)) {
                throw new MessageSendException(
                        FailureType.UNKNOWN,
                        "Telegram returned an unsuccessful response."
                );
            }

            JsonNode messageId = root.path("result").path("message_id");

            if (messageId.isMissingNode() || messageId.isNull()) {
                throw new MessageSendException(
                        FailureType.UNKNOWN,
                        "Telegram response contains no message ID."
                );
            }

            return messageId.asString();

        } catch (MessageSendException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MessageSendException(
                    FailureType.UNKNOWN,
                    "Could not parse Telegram response.",
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
