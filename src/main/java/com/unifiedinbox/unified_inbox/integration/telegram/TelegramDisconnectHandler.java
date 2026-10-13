
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.integration.IntegrationDisconnectHandler;
import org.springframework.stereotype.Component;

@Component
public class TelegramDisconnectHandler
        implements IntegrationDisconnectHandler {

    private final TelegramCredentialsRepository credentialsRepository;

    public TelegramDisconnectHandler(
            TelegramCredentialsRepository credentialsRepository
    ) {
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public String getType() {
        return "TELEGRAM";
    }

    @Override
    public void disconnect(IntegrationEntity integration) {
        credentialsRepository.findByIntegrationId(integration.getId())
                .ifPresent(credentialsRepository::delete);
    }
}
