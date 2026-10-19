package com.unifiedinbox.unified_inbox.integration;

public record IntegrationNextAction(
        String type,
        String url,
        Integer intervalMs
) {
}