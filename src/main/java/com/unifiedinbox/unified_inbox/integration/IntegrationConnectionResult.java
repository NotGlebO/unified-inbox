
package com.unifiedinbox.unified_inbox.integration;

public record IntegrationConnectionResult(
        Long integrationId,
        String status,
        IntegrationView view,
        IntegrationNextAction nextAction
) {
}
