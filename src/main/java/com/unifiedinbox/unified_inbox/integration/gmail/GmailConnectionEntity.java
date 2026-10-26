package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "gmail_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_google_account_id",
                        columnNames = "google_account_id"
                ),
                @UniqueConstraint(
                        name = "uq_gmail_integration",
                        columnNames = "integration_id"
                )
        }
)
public class GmailConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "integration_id", nullable = false, unique = true)
    private IntegrationEntity integration;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "google_account_id", nullable = false)
    private String googleAccountId;

    @Column(name = "encrypted_refresh_token", nullable = false)
    private String encryptedRefreshToken;

    @Column(name = "history_id")
    private String historyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GmailConnectionEntity() {
    }

    public GmailConnectionEntity(
            IntegrationEntity integration,
            String email,
            String googleAccountId,
            String encryptedRefreshToken
    ) {
        this.integration = integration;
        this.email = email;
        this.googleAccountId = googleAccountId;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public IntegrationEntity getIntegration() {
        return integration;
    }

    public String getEmail() {
        return email;
    }

    public String getGoogleAccountId() {
        return googleAccountId;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
