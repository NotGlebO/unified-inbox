
package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.*;
import com.unifiedinbox.unified_inbox.repository.GmailAccountRepository;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class GmailSenderService {

    private final GmailClientService gmailClientService;
    private final GmailAccountRepository gmailAccountRepository;
    private final RestClient restClient = RestClient.create();

    public GmailSenderService(
            GmailClientService gmailClientService,
            GmailAccountRepository gmailAccountRepository,
            MessageRepository messageRepository
    ) {
        this.gmailClientService = gmailClientService;
        this.gmailAccountRepository = gmailAccountRepository;
    }

    public String sendReply(
            ConversationEntity conversation,
            String text
    ) {
        IntegrationEntity integration = conversation.getIntegration();

        if (!integration.isEnabled()
                || integration.getType() != IntegrationType.GMAIL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Gmail integration is not available"
            );
        }

        GmailAccountEntity account = gmailAccountRepository
                .findByIntegrationId(integration.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gmail account not found"
                ));

        String threadId = conversation.getExternalChatId();

        // Retrieve the thread from Gmail to get reply headers
        String accessToken = gmailClientService.getAccessToken(account);

        Map<?, ?> thread = restClient.get()
                .uri("https://gmail.googleapis.com/gmail/v1/users/me/threads/{id}?format=full",
                        threadId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);

        if (thread == null || !(thread.get("messages") instanceof java.util.List<?> messages)
                || messages.isEmpty()) {
            throw new IllegalStateException("Gmail thread is empty");
        }

        Map<?, ?> lastMessage = (Map<?, ?>) messages.get(messages.size() - 1);
        Map<?, ?> payload = (Map<?, ?>) lastMessage.get("payload");

        String messageIdHeader = header(payload, "Message-ID");
        String references = header(payload, "References");
        String subject = header(payload, "Subject");

        // Find the most recent incoming message in this thread
        String recipient = null;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<?, ?> item = (Map<?, ?>) messages.get(i);
            Map<?, ?> itemPayload = (Map<?, ?>) item.get("payload");

            String from = header(itemPayload, "From");

            if (from != null
                    && !extractEmail(from).equalsIgnoreCase(account.getEmail())) {
                recipient = extractEmail(from);
                break;
            }
        }

        if (recipient == null) {
            throw new IllegalStateException(
                    "Could not determine Gmail reply recipient"
            );
        }

        if (subject == null) {
            subject = "";
        }

        if (!subject.toLowerCase(java.util.Locale.ROOT).startsWith("re:")) {
            subject = "Re: " + subject;
        }

        StringBuilder mime = new StringBuilder();

        mime.append("From: ").append(account.getEmail()).append("\r\n");
        mime.append("To: ").append(recipient).append("\r\n");
        mime.append("Subject: ")
                .append(encodeSubject(subject)).append("\r\n");
        mime.append("MIME-Version: 1.0\r\n");
        mime.append("Content-Type: text/plain; charset=UTF-8\r\n");
        mime.append("Content-Transfer-Encoding: base64\r\n");

        if (messageIdHeader != null) {
            mime.append("In-Reply-To: ")
                    .append(messageIdHeader).append("\r\n");

            mime.append("References: ");

            if (references != null && !references.isBlank()) {
                mime.append(references).append(" ");
            }

            mime.append(messageIdHeader).append("\r\n");
        }

        mime.append("\r\n");

        mime.append(Base64.getMimeEncoder(76,
                        "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(text.getBytes(StandardCharsets.UTF_8)));

        String raw = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        mime.toString().getBytes(StandardCharsets.UTF_8)
                );

        Map<?, ?> response = restClient.post()
                .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                .headers(h -> h.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "raw", raw,
                        "threadId", threadId
                ))
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("id") == null) {
            throw new IllegalStateException("Gmail did not confirm sending");
        }

        return response.get("id").toString();
    }

    private String header(Map<?, ?> payload, String name) {
        if (payload == null
                || !(payload.get("headers") instanceof java.util.List<?> headers)) {
            return null;
        }

        for (Object item : headers) {
            if (item instanceof Map<?, ?> h
                    && name.equalsIgnoreCase(String.valueOf(h.get("name")))) {
                return String.valueOf(h.get("value"));
            }
        }

        return null;
    }

    private String extractEmail(String value) {
        int start = value.lastIndexOf('<');
        int end = value.lastIndexOf('>');

        if (start >= 0 && end > start) {
            return value.substring(start + 1, end).trim();
        }

        return value.trim();
    }

    private String encodeSubject(String subject) {
        return "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(
                        subject.getBytes(StandardCharsets.UTF_8))
                + "?=";
    }
}
