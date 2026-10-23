
package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.ConversationReadStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationReadStateRepository
        extends JpaRepository<ConversationReadStateEntity, Long> {

    Optional<ConversationReadStateEntity>
    findByConversationIdAndUserId(
            Long conversationId,
            Long userId
    );

    List<ConversationReadStateEntity>
    findByUserIdAndConversationIdIn(
            Long userId,
            List<Long> conversationIds
    );
}
