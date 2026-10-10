
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationMemberEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationRole;
import com.unifiedinbox.unified_inbox.entity.UserEntity;

import com.unifiedinbox.unified_inbox.repository.OrganizationMemberRepository;
import com.unifiedinbox.unified_inbox.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationMemberService {

    private final OrganizationService organizationService;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public OrganizationMemberService(
            OrganizationService organizationService,
            OrganizationMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.organizationService = organizationService;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addMember(Long organizationId, String username) {

        // Only OWNER can add members
        OrganizationMemberEntity ownerMembership =
                organizationService.requireOwner(organizationId);

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }

        String normalizedUsername = username.trim();

        // Find registered user
        UserEntity user = userRepository
                .findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found."
                ));

        // Check if user is already a member
        if (memberRepository.existsByOrganizationIdAndUserId(
                organizationId,
                user.getId()
        )) {
            throw new IllegalArgumentException(
                    "User is already a member of this organization."
            );
        }

        OrganizationEntity organization =
                ownerMembership.getOrganization();

        OrganizationMemberEntity membership =
                new OrganizationMemberEntity(
                        organization,
                        user,
                        OrganizationRole.MEMBER
                );

        memberRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberEntity> getMembers(
            Long organizationId
    ) {

        // Only organization members can see the list
        organizationService.requireMember(organizationId);

        return memberRepository.findByOrganizationId(organizationId);
    }
}
