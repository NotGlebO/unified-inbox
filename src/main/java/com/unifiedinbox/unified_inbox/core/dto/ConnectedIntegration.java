package com.unifiedinbox.unified_inbox.core.dto;

public record ConnectedIntegration(
        Long id,
        String type,
        String displayName,
        String iconPath,
        boolean enabled,
        boolean disconnectable,
        String accountLabel
) {
}