
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.integration.webhook.WebhookHandler;
import com.unifiedinbox.unified_inbox.service.MessageService;
import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import com.unifiedinbox.unified_inbox.core.dto.UnifiedMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.service.MessageAttachmentService;

import java.time.Instant;
import java.util.Map;

@Component
public class TelegramWebhookHandler implements WebhookHandler {

    private final TelegramApiClient telegramApiClient;
    private final TelegramCredentialsService credentialsService;
    private final MessageAttachmentService attachmentService;
    private final TelegramCredentialsRepository credentialsRepository;
    private final TelegramWebhookSecretService secretService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public TelegramWebhookHandler(
            TelegramCredentialsRepository credentialsRepository,
            TelegramWebhookSecretService secretService,
            MessageService messageService,
            ObjectMapper objectMapper,
            TelegramApiClient telegramApiClient,
            TelegramCredentialsService credentialsService,
            MessageAttachmentService attachmentService
    ) {
        this.credentialsRepository = credentialsRepository;
        this.secretService = secretService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.telegramApiClient = telegramApiClient;
        this.credentialsService = credentialsService;
        this.attachmentService = attachmentService;
    }

    @Override
    public String getProvider() {
        return "TELEGRAM";
    }

    @Override
    public ResponseEntity<Void> handle(
            String body,
            Map<String, String> headers
    ) {
        String secret = headers.get("x-telegram-bot-api-secret-token");

        if (secret == null || !secret.matches("[A-Za-z0-9_-]{1,256}")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String secretHash = secretService.hash(secret);

        TelegramCredentialsEntity credentials = credentialsRepository
                .findByWebhookSecretHash(secretHash)
                .orElse(null);

        if (credentials == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var integration = credentials.getIntegration();

        if (!"TELEGRAM".equals(integration.getType())
                || !integration.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            JsonNode update = objectMapper.readTree(body);
            JsonNode message = update.path("message");

            if (message.isMissingNode() || message.isNull()) {
                return ResponseEntity.ok().build();
            }

            boolean hasAttachment =
                    message.has("photo")
                    || message.has("video")
                    || message.has("animation")
                    || message.has("document")
                    || message.has("audio")
                    || message.has("voice")
                    || message.has("video_note");

            boolean hasText =
                    message.path("text").isTextual()
                    || message.path("caption").isTextual();

            if (!hasText && !hasAttachment) {
                return ResponseEntity.ok().build();
            }

            String chatId = message.path("chat").path("id").asString();
            String messageId = message.path("message_id").asString();
            String text = message.path("text")
                .asString(message.path("caption").asString(""));
            TelegramAttachment attachment = extractAttachment(message);

            if (attachment != null) {
                String token = credentialsService.getTokenForSending(
                        integration.getId()
                );

                try {
                    telegramApiClient.getFilePath(token, attachment.fileId());
                } catch (TelegramFileTooLargeException exception) {
                    text = text.isBlank()
                            ? "📎 File is too large to download from Telegram."
                            : text + "\n📎 File is too large to download from Telegram.";

                    attachment = null;
                }
            }
            long timestamp = message.path("date").asLong();

            JsonNode from = message.path("from");

            String firstName = from.path("first_name").asString("");
            String lastName = from.path("last_name").asString("");

            String senderName = (firstName + " " + lastName).trim();

            UnifiedMessage unifiedMessage = new UnifiedMessage(
                    integration.getId(),
                    chatId,
                    messageId,
                    senderName,
                    text,
                    Instant.ofEpochSecond(timestamp),
                    MessageDirection.INCOMING
            );

            MessageEntity savedMessage =
                messageService.saveExternalMessage(unifiedMessage);

            if (savedMessage != null) {


                if (attachment != null) {

                    String token = credentialsService.getTokenForSending(
                            integration.getId()
                    );

                    String filePath = telegramApiClient.getFilePath(
                            token,
                            attachment.fileId()
                    );

                    byte[] content = telegramApiClient.downloadFile(
                            token,
                            filePath
                    );

                    attachmentService.saveIncomingAttachment(
                            savedMessage,
                            attachment.fileName(),
                            attachment.contentType(),
                            content
                    );
                }
            }

            return ResponseEntity.ok().build();

        } catch (JacksonException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    private TelegramAttachment extractAttachment(JsonNode message) {

        JsonNode file;
        String fileName;
        String contentType;

        if (message.has("photo")) {

            JsonNode photos = message.path("photo");

            if (!photos.isArray() || photos.isEmpty()) {
                return null;
            }

            // Telegram присылает несколько размеров фотографии.
            // Берём последний вариант.
            file = photos.get(photos.size() - 1);

            fileName = "photo.jpg";
            contentType = "image/jpeg";

        } else if (message.has("animation")) {

            file = message.path("animation");
            fileName = file.path("file_name").asString("animation.mp4");
            contentType = file.path("mime_type").asString("video/mp4");

        } else if (message.has("video")) {

            file = message.path("video");
            fileName = file.path("file_name").asString("video.mp4");
            contentType = file.path("mime_type").asString("video/mp4");

        } else if (message.has("document")) {

            file = message.path("document");
            fileName = file.path("file_name").asString("document");
            contentType = file.path("mime_type")
                    .asString("application/octet-stream");

        } else if (message.has("audio")) {

            file = message.path("audio");
            fileName = file.path("file_name").asString("audio.mp3");
            contentType = file.path("mime_type").asString("audio/mpeg");

        } else if (message.has("voice")) {

            file = message.path("voice");
            fileName = "voice.ogg";
            contentType = file.path("mime_type").asString("audio/ogg");

        } else if (message.has("video_note")) {

            file = message.path("video_note");
            fileName = "video_note.mp4";
            contentType = "video/mp4";

        } else {
            return null;
        }

        String fileId = file.path("file_id").asString("");

        if (fileId.isBlank()) {
            return null;
        }

        return new TelegramAttachment(
                fileId,
                fileName,
                contentType
        );
    }

    private record TelegramAttachment(
            String fileId,
            String fileName,
            String contentType
    ) {}
}
