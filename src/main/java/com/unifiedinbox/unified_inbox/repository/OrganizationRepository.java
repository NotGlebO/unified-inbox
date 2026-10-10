
package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository
        extends JpaRepository<OrganizationEntity, Long> {

    List<OrganizationEntity> findByOwnerId(Long ownerId);
}
