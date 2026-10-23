
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.adapter.AdapterRegistry;
import com.unifiedinbox.unified_inbox.core.adapter.MessagingAdapter;
import com.unifiedinbox.unified_inbox.core.dto.ConversationSummary;
import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.ConversationReadStateEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.UserEntity;
import com.unifiedinbox.unified_inbox.repository.ConversationReadStateRepository;
import com.unifiedinbox.unified_inbox.repository.ConversationRepository;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ConversationService {
    

    private final MessageRepository messageRepository;
    private final ConversationReadStateRepository readStateRepository;

    private final ConversationRepository conversationRepository;
    private final OrganizationService organizationService;
    private final AdapterRegistry adapterRegistry;

    public ConversationService(
            ConversationRepository conversationRepository,
            OrganizationService organizationService,
            AdapterRegistry adapterRegistry,
            MessageRepository messageRepository,
            ConversationReadStateRepository readStateRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.organizationService = organizationService;
        this.adapterRegistry = adapterRegistry;
        this.messageRepository = messageRepository;
        this.readStateRepository = readStateRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getOrganizationConversations(
            Long organizationId
    ) {

        organizationService.requireMember(organizationId);

        
        Long userId = organizationService.getCurrentUser().getId();

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

                    long lastReadId = readStateRepository
                            .findByConversationIdAndUserId(
                                    conversation.getId(),
                                    userId
                            )
                            .map(state -> state.getLastReadMessageId())
                            .orElse(0L);

                    long unreadCount = messageRepository.countUnread(
                            conversation.getId(),
                            lastReadId,
                            MessageDirection.INCOMING
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
                            conversation.getUpdatedAt(),
                            unreadCount
                    );
                })
                .toList();


        

    }
    
    @Transactional
    public void markAsRead(Long conversationId) {

        ConversationEntity conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Conversation not found"
                        )
                );

        Long organizationId = conversation
                .getIntegration()
                .getOrganization()
                .getId();

        organizationService.requireMember(organizationId);

        UserEntity user = organizationService.getCurrentUser();

        Long lastMessageId = messageRepository
                .findLastMessageId(conversationId);

        if (lastMessageId == null) {
            return;
        }

        ConversationReadStateEntity state = readStateRepository
                .findByConversationIdAndUserId(
                        conversationId,
                        user.getId()
                )
                .orElseGet(() ->
                        new ConversationReadStateEntity(
                                conversation,
                                user
                        )
                );

        state.markReadThrough(lastMessageId);
        readStateRepository.save(state);
    }


}
