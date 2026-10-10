
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;

import com.unifiedinbox.unified_inbox.integration.telegram.TelegramSenderService;
import com.unifiedinbox.unified_inbox.entity.IntegrationType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ConversationMessageService {

    private final TelegramSenderService telegramSenderService;
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    public ConversationMessageService(
            MessageRepository messageRepository,
            ConversationService conversationService,
            TelegramSenderService telegramSenderService
    ) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.telegramSenderService = telegramSenderService;
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

    
        public MessageEntity sendTelegramMessage(
                Long organizationId,
                Long conversationId,
                String text
        ) {
            // Check that the user can access this conversation
            ConversationEntity conversation =
                    conversationService.getConversation(
                            organizationId,
                            conversationId
                    );

            if (text == null || text.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Message cannot be empty."
                );
            }

            String messageText = text.trim();

            if (messageText.length() > 4096) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Message is too long."
                );
            }

            IntegrationEntity integration =
                    conversation.getIntegration();

            if (!integration.isEnabled()
                    || integration.getType() != IntegrationType.TELEGRAM) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Telegram integration is not available."
                );
            }

            Long chatId = conversation.getExternalChatId();

            // Send the message to Telegram first
            Long telegramMessageId = telegramSenderService.sendMessage(
                    integration,
                    chatId,
                    messageText
            );

            // Save the message only after Telegram accepts it
            MessageEntity message = new MessageEntity(
                    telegramMessageId,
                    chatId,
                    "Support",
                    messageText,
                    Instant.now()
            );

            message.setDirection("OUTGOING");
            message.setConversation(conversation);

            return messageRepository.save(message);
        }

}
