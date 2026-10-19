package com.unifiedinbox.unified_inbox.integration;

import java.util.Map;

public interface IntegrationConnectionHandler {

    String getType();

    IntegrationConnectionResult connect(
            Long organizationId,
            Map<String, String> credentials
    );
}
