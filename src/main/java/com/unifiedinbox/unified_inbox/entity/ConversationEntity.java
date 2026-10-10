
package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "conversations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_conversation_integration_chat",
            columnNames = {"integration_id", "external_chat_id"}
        )
    }
)
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "integration_id", nullable = false)
    private IntegrationEntity integration;

    @Column(name = "external_chat_id", nullable = false)
    private Long externalChatId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {
    }

    public ConversationEntity(
            OrganizationEntity organization,
            IntegrationEntity integration,
            Long externalChatId,
            String customerName
    ) {
        this.organization = organization;
        this.integration = integration;
        this.externalChatId = externalChatId;
        this.customerName = customerName;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public IntegrationEntity getIntegration() {
        return integration;
    }

    public Long getExternalChatId() {
        return externalChatId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
