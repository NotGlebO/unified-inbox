
package com.unifiedinbox.unified_inbox.integration;

import java.util.List;

public record IntegrationSetup(
        String title,
        String description,
        String connectionType,
        String submitUrl,
        List<IntegrationField> fields,
        List<String> instructions
) {
}
