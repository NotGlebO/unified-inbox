package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface MessageRepository
        extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity>
    findByConversationIdOrderBySentAtAscIdAsc(
            Long conversationId
    );

    boolean existsByConversationIdAndExternalMessageId(
            Long conversationId,
            String externalMessageId
    );

    
    @Query("""
    SELECT m.conversation.id, COUNT(m)
    FROM MessageEntity m
    WHERE m.conversation.id IN :conversationIds
    AND m.direction = :direction
    AND m.id > 0
    GROUP BY m.conversation.id
    """)
    List<Object[]> countIncomingByConversations(
            @Param("conversationIds") List<Long> conversationIds,
            @Param("direction") MessageDirection direction
    );

    @Query("""
    SELECT COUNT(m)
    FROM MessageEntity m
    WHERE m.conversation.id = :conversationId
    AND m.direction = :direction
    AND m.id > :lastReadMessageId
    """)
    long countUnread(
            @Param("conversationId") Long conversationId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("direction") MessageDirection direction
    );

    @Query("""
    SELECT MAX(m.id)
    FROM MessageEntity m
    WHERE m.conversation.id = :conversationId
    """)
    Long findLastMessageId(
            @Param("conversationId") Long conversationId
    );

}