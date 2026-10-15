package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.entity.MessageAttachmentEntity;
import com.unifiedinbox.unified_inbox.service.MessageAttachmentService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageAttachmentController {

    private final MessageAttachmentService attachmentService;

    public MessageAttachmentController(
            MessageAttachmentService attachmentService
    ) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(
            value = "/{messageId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadAttachment(
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        MessageAttachmentEntity attachment =
                attachmentService.upload(messageId, file);

        return ResponseEntity.ok(Map.of(
                "id", attachment.getId(),
                "fileName", attachment.getFileName(),
                "contentType", attachment.getContentType(),
                "fileSize", attachment.getFileSize()
        ));
    }
}