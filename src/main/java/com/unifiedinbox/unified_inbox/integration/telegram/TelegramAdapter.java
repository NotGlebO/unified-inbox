
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.core.adapter.MessagingAdapter;
import com.unifiedinbox.unified_inbox.core.dto.IntegrationCapabilities;

import org.springframework.stereotype.Component;

@Component
public class TelegramAdapter implements MessagingAdapter {

    private final TelegramCredentialsService credentialsService;
    private final TelegramApiClient telegramApiClient;

    public TelegramAdapter(
            TelegramCredentialsService credentialsService,
            TelegramApiClient telegramApiClient
    ) {
        this.credentialsService = credentialsService;
        this.telegramApiClient = telegramApiClient;
    }

    @Override
    public String getType() {
        return "TELEGRAM";
    }

    @Override
    public String getDisplayName() {
        return "Telegram";
    }

    @Override
    public String getIconPath() {
        return "/icons/telegram.svg";
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
        String token = credentialsService
                .getTokenForSending(integrationId);

        return telegramApiClient.sendMessage(
                token,
                externalChatId,
                text
        );
    }

    @Override
    public String getColor() {
        return "#26A5E4";
    }
}
