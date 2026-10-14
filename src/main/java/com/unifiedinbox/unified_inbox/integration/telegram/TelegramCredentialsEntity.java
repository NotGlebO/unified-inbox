
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "telegram_credentials",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_telegram_bot_id",
                columnNames = "bot_id"
        )
)
public class TelegramCredentialsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "integration_id", nullable = false, unique = true)
    private IntegrationEntity integration;

    @Column(name = "bot_id", nullable = false)
    private Long botId;

    @Column(name = "bot_username", nullable = false)
    private String botUsername;

    @Column(name = "encrypted_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedToken;
    
    @Column(name = "webhook_secret_hash", length = 64)
    private String webhookSecretHash;

    protected TelegramCredentialsEntity() {
    }

    public TelegramCredentialsEntity(
            IntegrationEntity integration,
            Long botId,
            String botUsername,
            String encryptedToken,
            String webhookSecretHash
    ) {
        this.integration = integration;
        this.botId = botId;
        this.botUsername = botUsername;
        this.encryptedToken = encryptedToken;
        this.webhookSecretHash = webhookSecretHash;
    }

    public Long getId() {
        return id;
    }

    public IntegrationEntity getIntegration() {
        return integration;
    }

    public Long getBotId() {
        return botId;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public String getWebhookSecretHash() {
        return webhookSecretHash;
    }
}
