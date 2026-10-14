
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.OutgoingMessageData;
import com.unifiedinbox.unified_inbox.core.dto.MessageStatus;
import com.unifiedinbox.unified_inbox.entity.OutboxEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.repository.OutboxRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxDataService {

    private final OutboxRepository outboxRepository;

    public OutboxDataService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(readOnly = true)
    public OutgoingMessageData getMessageData(Long outboxId) {

        OutboxEntity outbox = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        MessageEntity message = outbox.getMessage();

        if (message.getStatus() != MessageStatus.SENDING) {
            throw new IllegalStateException(
                    "Message is not ready for sending."
            );
        }

        ConversationEntity conversation = message.getConversation();
        IntegrationEntity integration = conversation.getIntegration();

        return new OutgoingMessageData(
                outbox.getId(),
                integration.getId(),
                integration.getType(),
                conversation.getExternalChatId(),
                message.getText(),
                integration.isEnabled()
        );
    }
}
