package com.unifiedinbox.unified_inbox.entity;

import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import jakarta.persistence.*;
import com.unifiedinbox.unified_inbox.core.dto.MessageStatus;
import com.unifiedinbox.unified_inbox.core.dto.MessageSource;


import java.time.Instant;

@Entity
@Table(
    name = "messages",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_message_conversation_external",
        columnNames = {"conversation_id", "external_message_id"}
    )
)
public class MessageEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status = MessageStatus.RECEIVED;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageDirection direction;

    protected MessageEntity() {
    }

    public MessageEntity(
            ConversationEntity conversation,
            String externalMessageId,
            String senderName,
            String text,
            Instant sentAt,
            MessageDirection direction
    ) {
        this.conversation = conversation;
        this.externalMessageId = externalMessageId;
        this.senderName = senderName;
        this.text = text;
        this.sentAt = sentAt;
        this.direction = direction;
    }

    public Long getId() {
        return id;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }

    
  
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private MessageSource origin = MessageSource.UNKNOWN;


    
    public MessageSource getOrigin() {
        return origin;
    }

    public void setOrigin(MessageSource origin) {
        this.origin = origin;
    }


}