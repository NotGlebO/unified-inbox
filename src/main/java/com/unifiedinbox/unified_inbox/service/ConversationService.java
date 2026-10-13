
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.ConversationSummary;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationMemberEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationRole;
import com.unifiedinbox.unified_inbox.entity.UserEntity;
import com.unifiedinbox.unified_inbox.repository.ConversationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final OrganizationService organizationService;

    public ConversationService(
            ConversationRepository conversationRepository,
            OrganizationService organizationService
    ) {
        this.conversationRepository = conversationRepository;
        this.organizationService = organizationService;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getOrganizationConversations(
            Long organizationId
    ) {

        // Check whether the current user belongs to this organization
        organizationService.requireMember(organizationId);

        // Load conversations belonging to the organization
        return conversationRepository
                .findAllByOrganizationId(organizationId)
                .stream()
                .map(conversation -> {

                    IntegrationEntity integration =
                            conversation.getIntegration();

                    return new ConversationSummary(
                            conversation.getId(),
                            integration.getId(),
                            integration.getType(),
                            integration.getDisplayName(),
                            conversation.getCustomerName(),
                            conversation.getUpdatedAt()
                    );
                })
                .toList();
    }

    
    @Transactional
    public void addMember(Long organizationId, String username) {

        // Only the organization owner can add members
        requireOwner(organizationId);

        // Validate the username
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }

        String normalizedUsername = username.trim();

        // Find an existing registered user
        UserEntity user = userRepository
                .findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found."
                ));

        // Check whether the user is already a member
        boolean alreadyMember = memberRepository
                .findByOrganizationIdAndUserId(
                        organizationId,
                        user.getId()
                )
                .isPresent();

        if (alreadyMember) {
            throw new IllegalArgumentException(
                    "This user is already a member of the organization."
            );
        }

        // Load the organization
        OrganizationEntity organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organization not found."
                ));

        // Create a new membership with MEMBER role
        OrganizationMemberEntity membership =
                new OrganizationMemberEntity(
                        organization,
                        user,
                        OrganizationRole.MEMBER
                );

        memberRepository.save(membership);
    }

}
