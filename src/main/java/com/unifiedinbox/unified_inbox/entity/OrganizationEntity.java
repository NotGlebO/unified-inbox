
package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organizations")
public class OrganizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OrganizationEntity() {
    }

    public OrganizationEntity(String name, UserEntity owner) {
        this.name = name;
        this.owner = owner;
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

    public String getName() {
        return name;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
