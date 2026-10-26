package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.core.adapter.MessagingAdapter;
import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException;
import com.unifiedinbox.unified_inbox.core.dto.IntegrationCapabilities;
import org.springframework.stereotype.Component;

@Component
public class GmailAdapter implements MessagingAdapter {

    private final GmailIntegrationService gmailService;
    private final GmailApiClient gmailApiClient;

    public GmailAdapter(
            GmailIntegrationService gmailService,
            GmailApiClient gmailApiClient
    ) {
        this.gmailService = gmailService;
        this.gmailApiClient = gmailApiClient;
    }

    @Override
    public String getType() {
        return "GMAIL";
    }

    @Override
    public String getDisplayName() {
        return "Gmail";
    }

    @Override
    public String getIconPath() {
        return "/icons/gmail.svg";
    }

    @Override
    public String getColor() {
        return "#EA4335";
    }

    @Override
    public IntegrationCapabilities getCapabilities() {
        return new IntegrationCapabilities(
                true,
                false,
                false,
                false,
                false,
                false
        );
    }

    @Override
    public String sendMessage(
            Long integrationId,
            String externalChatId,
            String text
    ) {
        String refreshToken = gmailService.getRefreshToken(integrationId);

        GmailApiClient.AccessTokenResponse tokenResponse = gmailApiClient.refreshAccessToken(refreshToken);
        String accessToken = tokenResponse.accessToken();

        String to = externalChatId;
        if (to == null || to.isBlank()) {
            throw new MessageSendException(
                    MessageSendException.FailureType.PERMANENT,
                    "Recipient email address is required."
            );
        }

        String subject = "Unified Inbox";
        String body = text;

        return gmailApiClient.sendMessage(accessToken, to, subject, body);
    }
}
