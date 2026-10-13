package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationRepository
        extends JpaRepository<IntegrationEntity, Long> {

    List<IntegrationEntity> findByOrganizationId(
            Long organizationId
    );

    List<IntegrationEntity> findByOrganizationIdAndEnabledTrue(
            Long organizationId
    );
}