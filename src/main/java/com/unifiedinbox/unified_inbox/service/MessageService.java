package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.UnifiedMessage;
import com.unifiedinbox.unified_inbox.entity.ConversationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.ConversationRepository;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unifiedinbox.unified_inbox.core.dto.MessageDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MessageService {
    private final OrganizationService organizationService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final IntegrationRepository integrationRepository;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            IntegrationRepository integrationRepository,
            OrganizationService organizationService
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
    }

    @Transactional
    public MessageEntity saveIncomingMessage(UnifiedMessage message) {

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
        return messageRepository
                .findByConversationIdOrderBySentAtAscIdAsc(conversationId)
                .stream()
                .map(message -> new MessageDetails(
                        message.getId(),
                        message.getSenderName(),
                        message.getText(),
                        message.getSentAt(),
                        message.getDirection()
                ))
                .toList();
    }
}