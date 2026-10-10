package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gmail_accounts")
public class GmailAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "integration_id",
            nullable = false,
            unique = true
    )
    private IntegrationEntity integration;

    @Column(name = "google_account_id", nullable = false)
    private String googleAccountId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(
            name = "encrypted_refresh_token",
            nullable = false,
            length = 2048
    )
    private String encryptedRefreshToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GmailAccountEntity() {
    }

    public GmailAccountEntity(
            IntegrationEntity integration,
            String googleAccountId,
            String email,
            String encryptedRefreshToken
    ) {
        this.integration = integration;
        this.googleAccountId = googleAccountId;
        this.email = email;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public IntegrationEntity getIntegration() {
        return integration;
    }

    public String getGoogleAccountId() {
        return googleAccountId;
    }

    public String getEmail() {
        return email;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }
}