package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.storage.FileStorageService;
import com.unifiedinbox.unified_inbox.entity.MessageAttachmentEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.MessageAttachmentRepository;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class MessageAttachmentService {

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    private final FileStorageService fileStorageService;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;

    public MessageAttachmentService(
            FileStorageService fileStorageService,
            MessageRepository messageRepository,
            MessageAttachmentRepository attachmentRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
    }

    public MessageAttachmentEntity upload(
            Long messageId,
            MultipartFile file
    ) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File exceeds maximum size of 10 MB"
            );
        }

        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Message not found")
                );

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String storageKey = fileStorageService.upload(
                file.getBytes(),
                contentType
        );

        MessageAttachmentEntity attachment = new MessageAttachmentEntity(
                message,
                file.getOriginalFilename() == null
                        ? "attachment"
                        : file.getOriginalFilename(),
                contentType,
                file.getSize(),
                storageKey
        );

        return attachmentRepository.save(attachment);

    }

    public MessageAttachmentEntity saveIncomingAttachment(
            MessageEntity message,
            String fileName,
            String contentType,
            byte[] content
    ) {
        if (message == null || message.getId() == null) {
            throw new IllegalArgumentException(
                    "Message must be saved before adding attachments"
            );
        }

        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Attachment is empty");
        }

        if (content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Attachment exceeds maximum allowed size"
            );
        }

        String safeContentType =
                contentType == null || contentType.isBlank()
                        ? "application/octet-stream"
                        : contentType;

        String safeFileName =
                fileName == null || fileName.isBlank()
                        ? "attachment"
                        : fileName;

        String storageKey = fileStorageService.upload(
                content,
                safeContentType
        );

        MessageAttachmentEntity attachment =
                new MessageAttachmentEntity(
                        message,
                        safeFileName,
                        safeContentType,
                        (long) content.length,
                        storageKey
                );

        return attachmentRepository.save(attachment);
    }
    
}