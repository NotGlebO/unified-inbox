package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "conversations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_conversation_integration_chat",
        columnNames = {"integration_id", "external_chat_id"}
    )
)
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "integration_id", nullable = false)
    private IntegrationEntity integration;

    @Column(name = "external_chat_id", nullable = false)
    private String externalChatId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {
    }

    public ConversationEntity(
            IntegrationEntity integration,
            String externalChatId,
            String customerName
    ) {
        this.integration = integration;
        this.externalChatId = externalChatId;
        this.customerName = customerName;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateLastActivity() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public IntegrationEntity getIntegration() {
        return integration;
    }

    public String getExternalChatId() {
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

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}