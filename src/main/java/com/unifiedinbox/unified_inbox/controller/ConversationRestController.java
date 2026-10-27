
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.core.dto.ConversationSummary;
import com.unifiedinbox.unified_inbox.core.dto.MessageDetails;
import com.unifiedinbox.unified_inbox.service.ConversationService;
import com.unifiedinbox.unified_inbox.service.MessageService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.unifiedinbox.unified_inbox.core.dto.SendMessageRequest;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.unifiedinbox.unified_inbox.core.dto.DeleteConversationsRequest;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ConversationRestController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationRestController(
            ConversationService conversationService,
            MessageService messageService
    ) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @GetMapping("/organizations/{organizationId}/conversations")
    public List<ConversationSummary> getConversations(
            @PathVariable Long organizationId
    ) {
        return conversationService
                .getOrganizationConversations(organizationId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageDetails> getMessages(
            @PathVariable Long conversationId
    ) {
        return messageService.getMessages(conversationId);
    }

    
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageDetails> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request
    ) {
        MessageEntity message = messageService.sendMessage(
                conversationId,
                request.text()
        );

        MessageDetails response = new MessageDetails(
                message.getId(),
                message.getSenderName(),
                message.getText(),
                message.getSentAt(),
                message.getDirection(),
                message.getStatus(),
                message.getOrigin(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
        }

    
        @PostMapping("/conversations/{conversationId}/read")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void markAsRead(
                @PathVariable Long conversationId
        ) {
        conversationService.markAsRead(conversationId);
    }

       
    @DeleteMapping("/messages/{messageId}/local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessageLocally(
            @PathVariable Long messageId
    ) {
    messageService.deleteMessageLocally(messageId);
    }

    @DeleteMapping("/conversations/{conversationId}/local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversationLocally(
            @PathVariable Long conversationId
    ) {
    conversationService.deleteConversationLocally(conversationId);
    }

    @PostMapping("/organizations/{organizationId}/conversations/delete-local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversationsLocally(
            @PathVariable Long organizationId,
            @RequestBody DeleteConversationsRequest request
    ) {
        conversationService.deleteConversationsLocally(
                organizationId,
                request.conversationIds()
        );
    }




}
