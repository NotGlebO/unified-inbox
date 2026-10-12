package com.unifiedinbox.unified_inbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class MessageEntity {

    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source", nullable = false)
    private String source = "TELEGRAM";


    @Column(name = "external_chat_id")
    private String externalChatId;

    @Column(name = "external_message_id")
    private String externalMessageId;
    
    @Column(name = "telegram_message_id")
    private Long telegramMessageId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "direction", nullable = false)
    private String direction = "INCOMING";

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private ConversationEntity conversation;


    public MessageEntity() {
    }

    public MessageEntity(
            Long telegramMessageId,
            Long chatId,
            String sender,
            String text,
            Instant receivedAt
    ) {
        this.telegramMessageId = telegramMessageId;
        this.chatId = chatId;
        this.sender = sender;
        this.text = text;
        this.receivedAt = receivedAt;
        this.source = "TELEGRAM";
        this.direction = "INCOMING";
    }

    public MessageEntity(
            String externalChatId,
            String externalMessageId,
            String sender,
            String text,
            Instant receivedAt
    ) {
        this.externalChatId = externalChatId;
        this.externalMessageId = externalMessageId;
        this.sender = sender;
        this.text = text;
        this.receivedAt = receivedAt;
        this.source = "WHATSAPP";
        this.direction = "INCOMING";
    }

        public MessageEntity(
        String gmailMessageId,
        String sender,
        String text,
        Instant receivedAt
    ) {
        this.externalMessageId = gmailMessageId;
        this.sender = sender;
        this.text = text;
        this.receivedAt = receivedAt;
        this.source = "GMAIL";
        this.direction = "INCOMING";
    }

    public Long getId() {
        return id;
    }

    public Long getTelegramMessageId() {
        return telegramMessageId;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    

    public String getSource() {
        return source;
    }
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getExternalChatId() {
        return externalChatId;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }
    
    public ConversationEntity getConversation() {
        return conversation;
    }

    public void setConversation(ConversationEntity conversation) {
        this.conversation = conversation;
    }


}