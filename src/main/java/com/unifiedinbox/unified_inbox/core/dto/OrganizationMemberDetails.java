package com.unifiedinbox.unified_inbox.core.dto;

import com.unifiedinbox.unified_inbox.entity.OrganizationRole;

public record OrganizationMemberDetails(
        Long id,
        String username,
        OrganizationRole role
) {
}