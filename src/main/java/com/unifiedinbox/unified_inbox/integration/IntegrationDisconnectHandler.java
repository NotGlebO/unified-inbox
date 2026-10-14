
package com.unifiedinbox.unified_inbox.integration;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;

public interface IntegrationDisconnectHandler {

    String getType();

    void disconnect(IntegrationEntity integration);
}
