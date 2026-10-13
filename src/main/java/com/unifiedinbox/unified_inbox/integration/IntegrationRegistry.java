
package com.unifiedinbox.unified_inbox.integration;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IntegrationRegistry {

    private final Map<String, IntegrationProvider> providers;

    public IntegrationRegistry(List<IntegrationProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        IntegrationProvider::getType,
                        Function.identity()
                ));
    }

    public List<IntegrationProvider> getAll() {
        return providers.values()
                .stream()
                .sorted(Comparator.comparing(
                        IntegrationProvider::getDisplayName
                ))
                .toList();
    }

    public IntegrationProvider getByType(String type) {
        return providers.get(type);
    }
}
