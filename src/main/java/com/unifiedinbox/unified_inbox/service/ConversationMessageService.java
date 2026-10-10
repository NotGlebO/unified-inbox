
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ConversationMessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    public ConversationMessageService(
            MessageRepository messageRepository,
            ConversationService conversationService
    ) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
    }

    @Transactional
    public void saveIncomingTelegramMessage(
            IntegrationEntity integration,
            Long telegramMessageId,
            Long chatId,
            String sender,
            String text,
            Instant receivedAt
    ) {
        if (telegramMessageId == null || chatId == null) {
            throw new IllegalArgumentException(
                    "Telegram message ID and chat ID are required."
            );
        }

        ConversationEntity conversation =
                conversationService.findOrCreateConversation(
                        integration,
                        chatId,
                        sender
                );

        boolean alreadyExists = messageRepository
                .existsByConversationIdAndTelegramMessageId(
                        conversation.getId(),
                        telegramMessageId
                );

        if (alreadyExists) {
            return;
        }

        MessageEntity message = new MessageEntity(
                telegramMessageId,
                chatId,
                sender,
                text,
                receivedAt
        );

        message.setConversation(conversation);

        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> getMessages(
            Long organizationId,
            Long conversationId
    ) {
        conversationService.getConversation(
                organizationId,
                conversationId
        );

        return messageRepository
                .findByConversationIdOrderByReceivedAtAsc(
                        conversationId
                );
    }
}
