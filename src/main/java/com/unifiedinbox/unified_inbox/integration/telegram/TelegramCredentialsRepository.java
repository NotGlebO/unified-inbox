
package com.unifiedinbox.unified_inbox.integration.telegram;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramCredentialsRepository
        extends JpaRepository<TelegramCredentialsEntity, Long> {

    Optional<TelegramCredentialsEntity> findByBotId(Long botId);

    Optional<TelegramCredentialsEntity> findByIntegrationId(
            Long integrationId
    );

    @EntityGraph(attributePaths = "integration")
    Optional<TelegramCredentialsEntity> findByWebhookSecretHash(
            String webhookSecretHash
    );
}
