
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.core.adapter.MessagingAdapter;
import com.unifiedinbox.unified_inbox.core.dto.IntegrationCapabilities;
import org.springframework.stereotype.Component;
import com.unifiedinbox.unified_inbox.service.EncryptionService;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TelegramAdapter implements MessagingAdapter {

    private final TelegramCredentialsRepository credentialsRepository;
    private final TelegramApiClient telegramApiClient;
    private final EncryptionService encryptionService;

    public TelegramAdapter(
            TelegramCredentialsRepository credentialsRepository,
            TelegramApiClient telegramApiClient,
            EncryptionService encryptionService
    ) {
        this.credentialsRepository = credentialsRepository;
        this.telegramApiClient = telegramApiClient;
        this.encryptionService = encryptionService;
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
                true,   // Text
                false,  // Images
                false,  // Files
                false,  // Stickers
                false,  // Editing
                false   // Reactions
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String sendMessage(
            Long integrationId,
            String externalChatId,
            String text
    ) {
        TelegramCredentialsEntity credentials = credentialsRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Telegram integration credentials not found."
                ));

        if (!credentials.getIntegration().isEnabled()) {
            throw new IllegalStateException(
                    "Telegram integration is disabled."
            );
        }

        String token = encryptionService.decrypt(
                credentials.getEncryptedToken()
        );

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
