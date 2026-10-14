
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.adapter.AdapterRegistry;
import com.unifiedinbox.unified_inbox.core.adapter.MessagingAdapter;
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
    private final AdapterRegistry adapterRegistry;

    public ConversationService(
            ConversationRepository conversationRepository,
            OrganizationService organizationService,
            AdapterRegistry adapterRegistry
    ) {
        this.conversationRepository = conversationRepository;
        this.organizationService = organizationService;
        this.adapterRegistry = adapterRegistry;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getOrganizationConversations(
            Long organizationId
    ) {

        organizationService.requireMember(organizationId);

        return conversationRepository
                .findAllByOrganizationId(organizationId)
                .stream()
                .map(conversation -> {

                    IntegrationEntity integration =
                            conversation.getIntegration();

                    MessagingAdapter adapter =
                            adapterRegistry.getAdapter(
                                    integration.getType()
                            );

                    return new ConversationSummary(
                            conversation.getId(),
                            integration.getId(),
                            adapter.getType(),
                            adapter.getDisplayName(),
                            adapter.getIconPath(),
                            adapter.getColor(),
                            integration.getDisplayName(),
                            conversation.getCustomerName(),
                            conversation.getUpdatedAt()
                    );
                })
                .toList();
    }
}
