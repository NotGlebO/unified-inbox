
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.ConversationSummary;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.repository.ConversationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
