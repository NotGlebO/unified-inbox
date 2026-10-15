package com.unifiedinbox.unified_inbox.core.dto;

public record AttachmentDetails(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        String contentUrl
) {}