package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}