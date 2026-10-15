package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.core.storage.FileStorageService;
import com.unifiedinbox.unified_inbox.entity.MessageAttachmentEntity;
import com.unifiedinbox.unified_inbox.service.MessageAttachmentService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentContentController {

    private final MessageAttachmentService attachmentService;
    private final FileStorageService fileStorageService;

    public AttachmentContentController(
            MessageAttachmentService attachmentService,
            FileStorageService fileStorageService
    ) {
        this.attachmentService = attachmentService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<byte[]> getContent(
            @PathVariable Long attachmentId
    ) {

        MessageAttachmentEntity attachment =
                attachmentService.getAuthorizedAttachment(attachmentId);

        byte[] content = fileStorageService.download(
                attachment.getStorageKey()
        );

        String contentType = attachment.getContentType();

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.length)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline"
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(content);
    }
}