package com.unifiedinbox.unified_inbox.integration;

public interface IntegrationProvider {

    String getType();

    String getDisplayName();

    String getIconPath();

    IntegrationSetup getSetup();
}
