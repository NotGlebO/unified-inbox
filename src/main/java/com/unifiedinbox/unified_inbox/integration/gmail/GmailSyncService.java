
package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import com.unifiedinbox.unified_inbox.core.dto.UnifiedMessage;
import com.unifiedinbox.unified_inbox.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class GmailSyncService {

    private final GmailApiClient gmailApiClient;
    private final GmailIntegrationService gmailIntegrationService;
    private final GmailConnectionRepository connectionRepository;
    private final MessageService messageService;

    public GmailSyncService(
            GmailApiClient gmailApiClient,
            GmailIntegrationService gmailIntegrationService,
            GmailConnectionRepository connectionRepository,
            MessageService messageService
    ) {
        this.gmailApiClient = gmailApiClient;
        this.gmailIntegrationService = gmailIntegrationService;
        this.connectionRepository = connectionRepository;
        this.messageService = messageService;
    }

    @Transactional
    public void sync(Long integrationId) {
        GmailConnectionEntity connection = connectionRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Gmail connection not found."
                ));

        if (!connection.getIntegration().isEnabled()) {
            throw new IllegalStateException(
                    "Gmail integration is disabled."
            );
        }

        String startHistoryId = connection.getHistoryId();

        if (startHistoryId == null || startHistoryId.isBlank()) {
            throw new IllegalStateException(
                    "Gmail history ID is not initialized. " +
                    "Register Gmail watch first."
            );
        }

        String refreshToken = gmailIntegrationService
                .getRefreshToken(integrationId);

        String accessToken = gmailApiClient
                .refreshAccessToken(refreshToken)
                .accessToken();

        String pageToken = null;
        String latestHistoryId = null;

        Set<String> processedMessageIds = new HashSet<>();

        do {
            JsonNode history = gmailApiClient.getHistory(
                    accessToken,
                    startHistoryId,
                    pageToken
            );

            JsonNode historyEntries = history.path("history");

            if (historyEntries.isArray()) {
                for (JsonNode entry : historyEntries) {
                    JsonNode addedMessages =
                            entry.path("messagesAdded");

                    if (!addedMessages.isArray()) {
                        continue;
                    }

                    for (JsonNode added : addedMessages) {
                        String messageId = added
                                .path("message")
                                .path("id")
                                .asString("");

                        if (messageId.isBlank()
                                || !processedMessageIds.add(messageId)) {
                            continue;
                        }

                        processMessage(
                                integrationId,
                                accessToken,
                                messageId
                        );
                    }
                }
            }

            String responseHistoryId = history
                    .path("historyId")
                    .asString("");

            if (!responseHistoryId.isBlank()) {
                latestHistoryId = responseHistoryId;
            }

            pageToken = history.path("nextPageToken")
                    .asString("");

        } while (!pageToken.isBlank());

        if (latestHistoryId == null) {
            throw new IllegalStateException(
                    "Gmail history response contains no historyId."
            );
        }

        // Update cursor only after all pages and messages
        // have been processed successfully.
        connection.setHistoryId(latestHistoryId);
    }

    private void processMessage(
            Long integrationId,
            String accessToken,
            String messageId
    ) {
        JsonNode message = gmailApiClient.getMessage(
                accessToken,
                messageId
        );

        // Only incoming inbox messages.
        boolean inbox = false;

        for (JsonNode label : message.path("labelIds")) {
            if ("INBOX".equals(label.asString())) {
                inbox = true;
                break;
            }
        }

        if (!inbox) {
            return;
        }

        String threadId = message.path("threadId").asString("");

        if (threadId.isBlank()) {
            throw new IllegalStateException(
                    "Gmail message has no thread ID."
            );
        }

        JsonNode payload = message.path("payload");

        String sender = getHeader(payload, "From");
        String subject = getHeader(payload, "Subject");

        String snippet = message.path("snippet").asString("");

        if (sender.isBlank()) {
            sender = "Unknown sender";
        }

        String text = subject.isBlank()
                ? snippet
                : subject + "\n\n" + snippet;

        String internalDate = message
                .path("internalDate")
                .asString("");

        if (internalDate.isBlank()) {
            throw new IllegalStateException(
                    "Gmail message has no internalDate."
            );
        }

        Instant receivedAt = Instant.ofEpochMilli(
                Long.parseLong(internalDate)
        );

        UnifiedMessage unifiedMessage = new UnifiedMessage(
                integrationId,
                threadId,
                messageId,
                sender,
                text,
                receivedAt,
                MessageDirection.INCOMING
        );

        messageService.saveExternalMessage(unifiedMessage);
    }

    private String getHeader(JsonNode payload, String name) {
        JsonNode headers = payload.path("headers");

        if (!headers.isArray()) {
            return "";
        }

        for (JsonNode header : headers) {
            if (name.equalsIgnoreCase(
                    header.path("name").asString("")
            )) {
                return header.path("value").asString("");
            }
        }

        return "";
    }
}
