package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "message_attachments")
public class MessageAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MessageAttachmentEntity() {
    }

    public MessageAttachmentEntity(
            MessageEntity message,
            String fileName,
            String contentType,
            Long fileSize,
            String storageKey
    ) {
        this.message = message;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageKey = storageKey;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public MessageEntity getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}