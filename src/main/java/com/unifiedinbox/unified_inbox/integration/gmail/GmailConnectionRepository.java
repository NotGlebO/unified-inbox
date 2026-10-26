package com.unifiedinbox.unified_inbox.integration.gmail;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GmailConnectionRepository
        extends JpaRepository<GmailConnectionEntity, Long> {

    Optional<GmailConnectionEntity> findByGoogleAccountId(String googleAccountId);

    Optional<GmailConnectionEntity> findByIntegrationId(Long integrationId);

    @EntityGraph(attributePaths = "integration")
    Optional<GmailConnectionEntity> findByEmail(String email);
}
