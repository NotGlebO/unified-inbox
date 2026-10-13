package com.unifiedinbox.unified_inbox.core.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AdapterRegistry {

    private final Map<String, MessagingAdapter> adapters;

    public AdapterRegistry(List<MessagingAdapter> adapterList) {

        this.adapters = adapterList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MessagingAdapter::getType,
                        Function.identity()
                ));
    }

    public MessagingAdapter getAdapter(String type) {

        MessagingAdapter adapter = adapters.get(type);

        if (adapter == null) {
            throw new IllegalArgumentException(
                    "Unsupported integration type: " + type
            );
        }

        return adapter;
    }
}