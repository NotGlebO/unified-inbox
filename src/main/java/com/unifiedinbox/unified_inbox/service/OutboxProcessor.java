
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.adapter.AdapterRegistry;
import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException;
import com.unifiedinbox.unified_inbox.entity.OutboxEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.repository.OutboxRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final AdapterRegistry adapterRegistry;
    private final OutboxCompletionService completionService;

    public OutboxProcessor(
            OutboxRepository outboxRepository,
            AdapterRegistry adapterRegistry,
            OutboxCompletionService completionService
    ) {
        this.outboxRepository = outboxRepository;
        this.adapterRegistry = adapterRegistry;
        this.completionService = completionService;
    }

    public void process(Long outboxId) {

        OutboxEntity outbox = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        MessageEntity message = outbox.getMessage();
        ConversationEntity conversation = message.getConversation();
        IntegrationEntity integration = conversation.getIntegration();

        if (!integration.isEnabled()) {
            throw new IllegalStateException(
                    "Integration is disabled."
            );
        }

        String externalMessageId;

        try {
            externalMessageId = adapterRegistry
                    .getAdapter(integration.getType())
                    .sendMessage(
                            integration.getId(),
                            conversation.getExternalChatId(),
                            message.getText()
                    );

        } catch (MessageSendException exception) {

            switch (exception.getFailureType()) {
                case UNKNOWN ->
                        completionService.markUnknown(
                                outboxId,
                                exception.getMessage()
                        );

                
                        case PERMANENT ->
                                completionService.markFailed(
                                        outboxId,
                                        exception.getMessage()
                                );

                        case RETRYABLE ->
                                completionService.markRetry(
                                        outboxId,
                                        exception.getMessage(),
                                        java.time.Instant.now().plusSeconds(30)
                                );

            }

            return;
        }

        completionService.markSent(
                outboxId,
                externalMessageId
        );
    }
}
