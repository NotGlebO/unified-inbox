package com.unifiedinbox.unified_inbox.core.dto;

public record IntegrationCapabilities(
        boolean supportsText,
        boolean supportsImages,
        boolean supportsFiles,
        boolean supportsStickers,
        boolean supportsEditing,
        boolean supportsReactions
) {
}