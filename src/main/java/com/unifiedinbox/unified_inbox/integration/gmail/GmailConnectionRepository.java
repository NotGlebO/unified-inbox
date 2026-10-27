package com.unifiedinbox.unified_inbox.integration.gmail;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;



public interface GmailConnectionRepository
        extends JpaRepository<GmailConnectionEntity, Long> {

    Optional<GmailConnectionEntity> findByGoogleAccountId(String googleAccountId);

    Optional<GmailConnectionEntity> findByIntegrationId(Long integrationId);

    @EntityGraph(attributePaths = "integration")
    @Query("SELECT c FROM GmailConnectionEntity c")
    Optional<GmailConnectionEntity> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("""
            UPDATE GmailConnectionEntity c
            SET c.historyId = :historyId
            WHERE c.id = :id
            AND c.historyId IS NULL
            """)
    int initializeHistoryId(
            @Param("id") Long id,
            @Param("historyId") String historyId
    );

    @EntityGraph(attributePaths = "integration")
    @Query("SELECT c FROM GmailConnectionEntity c")
    List<GmailConnectionEntity> findAllWithIntegration();
}
