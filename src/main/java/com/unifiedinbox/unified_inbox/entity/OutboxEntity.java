
package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "message_outbox")
public class OutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private MessageEntity message;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected OutboxEntity() {
    }

    public OutboxEntity(MessageEntity message) {
        this.message = message;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public MessageEntity getMessage() {
        return message;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    
    public void scheduleRetry(
            Instant nextAttemptAt,
            String error
    ) {
        this.attempts++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error;
    }

    public void recordFailure(String error) {
        this.attempts++;
        this.lastError = error;
    }

    public void recordUnknown(String error) {
        this.attempts++;
        this.lastError = error;
    }

    
    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void claim() {
        this.claimedAt = Instant.now();
    }

    public void clearClaim() {
        this.claimedAt = null;
    }


}
