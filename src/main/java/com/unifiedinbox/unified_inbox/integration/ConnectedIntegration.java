
package com.unifiedinbox.unified_inbox.integration;

public record ConnectedIntegration(
        Long id,
        String type,
        String displayName,
        String iconPath,
        boolean enabled
) {
}
