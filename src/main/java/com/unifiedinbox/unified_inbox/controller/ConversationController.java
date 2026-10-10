
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.service.ConversationService;
import com.unifiedinbox.unified_inbox.service.ConversationMessageService;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationMessageService messageService;

    public ConversationController(
            ConversationService conversationService,
            ConversationMessageService messageService
    ) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @GetMapping
    public List<ConversationResponse> getConversations(
            @PathVariable Long organizationId
    ) {
        return conversationService
                .getConversations(organizationId)
                .stream()
                .map(conversation -> new ConversationResponse(
                        conversation.getId(),
                        conversation.getCustomerName(),
                        conversation.getIntegration().getDisplayName()
                ))
                .toList();
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable Long organizationId,
            @PathVariable Long conversationId
    ) {
        return messageService
                .getMessages(organizationId, conversationId)
                .stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getSender(),
                        message.getText(),
                        message.getDirection(),
                        message.getReceivedAt()
                ))
                .toList();
    }

    
    @PostMapping("/{conversationId}/messages")
    public MessageResponse sendMessage(
            @PathVariable Long organizationId,
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request
    ) {
    var message = messageService.sendTelegramMessage(
            organizationId,
            conversationId,
            request.text()
    );

    return new MessageResponse(
            message.getId(),
            message.getSender(),
            message.getText(),
            message.getDirection(),
            message.getReceivedAt()
    );
    }


    public record ConversationResponse(
            Long id,
            String customerName,
            String integrationName
    ) {}
    public record SendMessageRequest(String text) {}

    public record MessageResponse(
            Long id,
            String sender,
            String text,
            String direction,
            Instant receivedAt
    ) {}
}
