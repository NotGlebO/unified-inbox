
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationMemberEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationRole;
import com.unifiedinbox.unified_inbox.entity.UserEntity;

import com.unifiedinbox.unified_inbox.repository.OrganizationRepository;
import com.unifiedinbox.unified_inbox.repository.OrganizationMemberRepository;
import com.unifiedinbox.unified_inbox.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.unifiedinbox.unified_inbox.dto.OrganizationSummary;


import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrganizationEntity createOrganization(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization name cannot be empty."
            );
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                    "Organization name cannot exceed 100 characters."
            );
        }

        UserEntity currentUser = getCurrentUser();

        // Create organization
        OrganizationEntity organization =
                new OrganizationEntity(normalizedName, currentUser);

        organization = organizationRepository.save(organization);

        // Add creator as OWNER
        OrganizationMemberEntity ownerMembership =
                new OrganizationMemberEntity(
                        organization,
                        currentUser,
                        OrganizationRole.OWNER
                );

        memberRepository.save(ownerMembership);

        return organization;
    }

  
    @Transactional(readOnly = true)
    public List<OrganizationSummary> getMyOrganizations() {

        UserEntity currentUser = getCurrentUser();

        return memberRepository.findByUserId(currentUser.getId())
                .stream()
                .map(membership -> new OrganizationSummary(
                        membership.getOrganization().getId(),
                        membership.getOrganization().getName(),
                        membership.getRole()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserEntity getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException(
                    "User is not authenticated."
            );
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found."
                ));
        }

    
        @Transactional(readOnly = true)
        public OrganizationMemberEntity requireMember(Long organizationId) {

        UserEntity currentUser = getCurrentUser();

        return memberRepository
                .findByOrganizationIdAndUserId(
                        organizationId,
                        currentUser.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organization not found."
                ));
        }

        @Transactional(readOnly = true)
        public OrganizationMemberEntity requireOwner(Long organizationId) {

        OrganizationMemberEntity membership =
                requireMember(organizationId);

        if (membership.getRole() != OrganizationRole.OWNER) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only the organization owner can perform this action."
                );
        }

        return membership;
        }

}
