
package com.unifiedinbox.unified_inbox.dto;

import com.unifiedinbox.unified_inbox.entity.OrganizationRole;

public record OrganizationSummary(
        Long id,
        String name,
        OrganizationRole role
) {
}
