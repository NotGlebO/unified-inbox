package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.UnifiedMessage;
import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.ConversationRepository;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;
import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import java.time.Instant;
import com.unifiedinbox.unified_inbox.core.dto.MessageSource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unifiedinbox.unified_inbox.core.dto.MessageDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.unifiedinbox.unified_inbox.core.dto.MessageStatus;
import com.unifiedinbox.unified_inbox.entity.OutboxEntity;
import com.unifiedinbox.unified_inbox.repository.OutboxRepository;



import com.unifiedinbox.unified_inbox.core.dto.AttachmentDetails;
import com.unifiedinbox.unified_inbox.repository.MessageAttachmentRepository;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;

@Service
public class MessageService {
    private final OutboxRepository outboxRepository;
    private final OrganizationService organizationService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final IntegrationRepository integrationRepository;
    private final MessageAttachmentRepository attachmentRepository;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            IntegrationRepository integrationRepository,
            OrganizationService organizationService,
            OutboxRepository outboxRepository,
            MessageAttachmentRepository attachmentRepository
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
        this.outboxRepository = outboxRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public MessageEntity saveExternalMessage(UnifiedMessage message) {

        // 1. Проверяем, существует ли интеграция
        IntegrationEntity integration = integrationRepository
                .findById(message.integrationId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Integration not found"
                        )
                );

        // 2. Проверяем, включена ли интеграция
        if (!integration.isEnabled()) {
            throw new IllegalStateException(
                    "Integration is disabled"
            );
        }

        // 3. Ищем переписку с этим клиентом
        ConversationEntity conversation = conversationRepository
                .findByIntegrationIdAndExternalChatId(
                        integration.getId(),
                        message.externalChatId()
                )
                .orElseGet(() -> conversationRepository.save(
                        new ConversationEntity(
                                integration,
                                message.externalChatId(),
                                message.senderName()
                        )
                ));

        // 4. Проверяем, не сохраняли ли сообщение раньше
        if (message.externalMessageId() != null &&
                messageRepository.existsByConversationIdAndExternalMessageId(
                        conversation.getId(),
                        message.externalMessageId()
                )) {
            return null;
        }

        // 5. Создаём Entity
        MessageEntity entity = new MessageEntity(
                conversation,
                message.externalMessageId(),
                message.senderName(),
                message.text(),
                message.sentAt(),
                message.direction()
        );

        
        if (message.direction() == MessageDirection.OUTGOING) {
            entity.setOrigin(MessageSource.EXTERNAL);
            entity.setStatus(MessageStatus.SENT);
        } else {
            entity.setOrigin(MessageSource.EXTERNAL);
            entity.setStatus(MessageStatus.RECEIVED);
        }


        // 6. Сохраняем сообщение
        MessageEntity saved = messageRepository.save(entity);

        



        // 7. Обновляем время активности переписки
        conversation.updateLastActivity();

        return saved;
    }

    @Transactional(readOnly = true)
    public List<MessageDetails> getMessages(Long conversationId) {
        

        // 1. Находим переписку
        ConversationEntity conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation not found."
                ));

        // 2. Определяем организацию переписки
        Long organizationId = conversation
                .getIntegration()
                .getOrganization()
                .getId();

        // 3. Проверяем права пользователя
        organizationService.requireMember(organizationId);

        // 4. Загружаем сообщения и преобразуем их в DTO
    // 4. Загружаем сообщения
List<MessageEntity> messages = messageRepository
        .findByConversationIdOrderBySentAtAscIdAsc(conversationId);

// 5. Загружаем вложения для всех сообщений одним запросом
List<Long> messageIds = messages.stream()
        .map(MessageEntity::getId)
        .toList();

Map<Long, List<AttachmentDetails>> attachmentsByMessage =
        new HashMap<>();

if (!messageIds.isEmpty()) {
    attachmentRepository.findByMessageIdIn(messageIds)
            .forEach(attachment -> {

                Long messageId = attachment.getMessage().getId();

                AttachmentDetails details = new AttachmentDetails(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getContentType(),
                        attachment.getFileSize(),
                        "/api/attachments/"
                                + attachment.getId()
                                + "/content"
                );

                attachmentsByMessage
                        .computeIfAbsent(
                                messageId,
                                ignored -> new ArrayList<>()
                        )
                        .add(details);
            });
}

                // 6. Формируем ответ для frontend
                return messages.stream()
                        .map(message -> new MessageDetails(
                                message.getId(),
                                message.getSenderName(),
                                message.getText(),
                                message.getSentAt(),
                                message.getDirection(),
                                message.getStatus(),
                                message.getOrigin(),
                                attachmentsByMessage.getOrDefault(
                                        message.getId(),
                                        List.of()
                                )
                        ))
                        .toList();
    }

    
  
    @Transactional
    public MessageEntity sendMessage(
            Long conversationId,
            String text
    ) {
        ConversationEntity conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation not found."
                ));

        IntegrationEntity integration = conversation.getIntegration();

        organizationService.requireMember(
                integration.getOrganization().getId()
        );

        if (!integration.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Integration is disabled."
            );
        }

        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot be empty."
            );
        }

        String normalizedText = text.trim();

        if (normalizedText.length() > 4096) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message is too long."
            );
        }

        
        MessageEntity message = new MessageEntity(
                conversation,
                null,
                "Operator",
                normalizedText,
                Instant.now(),
                MessageDirection.OUTGOING
        );

        message.setOrigin(MessageSource.WEB_APP);
        message.setStatus(MessageStatus.PENDING);
        ;

        MessageEntity saved = messageRepository.save(message);

        outboxRepository.save(new OutboxEntity(saved));

        conversation.updateLastActivity();

        return saved;
    }



}