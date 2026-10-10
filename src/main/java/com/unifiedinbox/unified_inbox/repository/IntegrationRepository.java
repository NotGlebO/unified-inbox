
package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationType;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface IntegrationRepository
        extends JpaRepository<IntegrationEntity, Long> {

    List<IntegrationEntity> findByOrganizationId(
            Long organizationId
    );

    List<IntegrationEntity> findByOrganizationIdAndEnabledTrue(
            Long organizationId
    );

    Optional<IntegrationEntity> findByTypeAndExternalBotId(
            IntegrationType type,
            Long externalBotId
    );

    Optional<IntegrationEntity> findByTypeAndExternalBotIdAndEnabledTrue(
            IntegrationType type,
            Long externalBotId
    );
}
