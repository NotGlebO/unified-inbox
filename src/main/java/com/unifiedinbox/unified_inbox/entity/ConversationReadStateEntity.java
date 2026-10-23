
package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "conversation_read_states",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_conversation_read_user",
        columnNames = {"conversation_id", "user_id"}
    )
)
public class ConversationReadStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "last_read_message_id", nullable = false)
    private Long lastReadMessageId = 0L;

    protected ConversationReadStateEntity() {
    }

    public ConversationReadStateEntity(
            ConversationEntity conversation,
            UserEntity user
    ) {
        this.conversation = conversation;
        this.user = user;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void markReadThrough(Long messageId) {
        if (messageId != null) {
            lastReadMessageId = Math.max(
                    lastReadMessageId,
                    messageId
            );
        }
    }
}
