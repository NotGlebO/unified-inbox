
package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.OrganizationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository
        extends JpaRepository<OrganizationMemberEntity, Long> {

    List<OrganizationMemberEntity> findByUserId(Long userId);

    List<OrganizationMemberEntity> findByOrganizationId(
            Long organizationId
    );

    Optional<OrganizationMemberEntity>
    findByOrganizationIdAndUserId(
            Long organizationId,
            Long userId
    );

    boolean existsByOrganizationIdAndUserId(
            Long organizationId,
            Long userId
    );
}
