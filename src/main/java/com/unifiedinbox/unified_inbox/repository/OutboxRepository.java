
package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

import java.util.Optional;

public interface OutboxRepository
        extends JpaRepository<OutboxEntity, Long> {

    @Query(value = """
            SELECT o.*
            FROM message_outbox o
            JOIN messages m ON m.id = o.message_id
            WHERE m.status IN ('PENDING', 'RETRY')
              AND o.next_attempt_at <= CURRENT_TIMESTAMP
            ORDER BY o.created_at ASC, o.id ASC
            LIMIT 1
            FOR UPDATE OF o SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxEntity> findNextPendingForUpdate();

    
    @Query(value = """
            SELECT o.*
            FROM message_outbox o
            JOIN messages m ON m.id = o.message_id
            WHERE m.status = 'SENDING'
            AND (
            o.claimed_at < :cutoff
            OR o.claimed_at IS NULL
            )
            ORDER BY o.id ASC
            LIMIT 1
            FOR UPDATE OF o SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxEntity> findNextStuckForUpdate(
            @org.springframework.data.repository.query.Param("cutoff")
            Instant cutoff
    );

}
