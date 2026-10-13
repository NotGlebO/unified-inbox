package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity>
    findByIntegrationIdOrderByUpdatedAtDesc(
            Long integrationId
    );

    Optional<ConversationEntity>
    findByIntegrationIdAndExternalChatId(
            Long integrationId,
            String externalChatId
    );
}