
package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "integrations")
public class IntegrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IntegrationType type;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "external_bot_id")
    private Long externalBotId;

    @Column(name = "encrypted_token", length = 1024)
    private String encryptedToken;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IntegrationEntity() {
    }

    
    public IntegrationEntity(
            OrganizationEntity organization,
            IntegrationType type,
            String displayName,
            Long externalBotId,
            String encryptedToken
    ) {
        this.organization = organization;
        this.type = type;
        this.displayName = displayName;
        this.externalBotId = externalBotId;
        this.encryptedToken = encryptedToken;
        this.enabled = true;
        this.createdAt = Instant.now();
    }


    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public IntegrationType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getExternalBotId() {
        return externalBotId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
