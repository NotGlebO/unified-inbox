package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException;
import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException.FailureType;
import com.unifiedinbox.unified_inbox.service.EncryptionService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramCredentialsService {

    private final TelegramCredentialsRepository credentialsRepository;
    private final EncryptionService encryptionService;

    public TelegramCredentialsService(
            TelegramCredentialsRepository credentialsRepository,
            EncryptionService encryptionService
    ) {
        this.credentialsRepository = credentialsRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public String getTokenForSending(Long integrationId) {

        TelegramCredentialsEntity credentials = credentialsRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new MessageSendException(
                        FailureType.PERMANENT,
                        "Telegram integration credentials not found."
                ));

        if (!credentials.getIntegration().isEnabled()) {
            throw new MessageSendException(
                    FailureType.PERMANENT,
                    "Telegram integration is disabled."
            );
        }

        try {
            return encryptionService.decrypt(
                    credentials.getEncryptedToken()
            );
        } catch (Exception exception) {
            throw new MessageSendException(
                    FailureType.PERMANENT,
                    "Could not decrypt Telegram credentials.",
                    exception
            );
        }
    }
}
