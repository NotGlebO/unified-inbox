package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    
    @Query("""
    SELECT c
    FROM ConversationEntity c
    JOIN FETCH c.integration i
    WHERE i.organization.id = :organizationId
    ORDER BY c.updatedAt DESC, c.id DESC
    """)
    List<ConversationEntity> findAllByOrganizationId(
            @Param("organizationId") Long organizationId
    );

}