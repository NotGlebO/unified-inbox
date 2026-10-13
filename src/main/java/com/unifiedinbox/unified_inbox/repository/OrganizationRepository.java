package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository
        extends JpaRepository<OrganizationEntity, Long> {
}