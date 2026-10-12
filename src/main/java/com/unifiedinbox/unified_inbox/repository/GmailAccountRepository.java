package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.GmailAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import java.util.Optional;

public interface GmailAccountRepository
        extends JpaRepository<GmailAccountEntity, Long> {

    Optional<GmailAccountEntity> findByIntegrationId(
            Long integrationId
    );

    Optional<GmailAccountEntity> findByGoogleAccountId(
            String googleAccountId
    );
    @Query("""
    SELECT a
    FROM GmailAccountEntity a
    JOIN FETCH a.integration i
    WHERE i.enabled = true
    """)
List<GmailAccountEntity> findAllActive();
}