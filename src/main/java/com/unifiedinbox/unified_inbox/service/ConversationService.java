
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
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

    @Transactional
        public ConversationEntity findOrCreateConversation(
                IntegrationEntity integration,
                String externalChatId,
                String customerName
        ) {
        if (!integration.isEnabled()) {
            throw new IllegalStateException(
                    "Integration is disconnected."
            );
        }

        if (externalChatId == null) {
            throw new IllegalArgumentException(
                    "External chat ID cannot be null."
            );
        }

        return conversationRepository
                .findByIntegrationIdAndExternalChatId(
                        integration.getId(),
                        externalChatId
                )
                .orElseGet(() -> {
                    ConversationEntity conversation =
                            new ConversationEntity(
                                    integration.getOrganization(),
                                    integration,
                                    externalChatId,
                                    customerName
                            );

                    return conversationRepository.save(conversation);
                });
    }

    @Transactional(readOnly = true)
    public List<ConversationEntity> getConversations(
            Long organizationId
    ) {
        organizationService.requireMember(organizationId);

        return conversationRepository.findByOrganizationId(
                organizationId
        );
    }

    @Transactional(readOnly = true)
    public ConversationEntity getConversation(
            Long organizationId,
            Long conversationId
    ) {
        organizationService.requireMember(organizationId);

        return conversationRepository
                .findByIdAndOrganizationId(
                        conversationId,
                        organizationId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation not found."
                ));
    }
}
