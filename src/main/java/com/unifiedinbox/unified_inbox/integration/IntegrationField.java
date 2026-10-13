package com.unifiedinbox.unified_inbox.integration;

public record IntegrationField(
        String name,
        String label,
        String type,
        String placeholder,
        boolean required
) {
}
