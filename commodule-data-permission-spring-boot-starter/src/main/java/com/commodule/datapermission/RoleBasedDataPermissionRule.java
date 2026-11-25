package com.commodule.datapermission;

import com.commodule.auth.AuthPrincipal;

public class RoleBasedDataPermissionRule implements DataPermissionRule {

    private final DataPermissionProperties properties;

    public RoleBasedDataPermissionRule(DataPermissionProperties properties) {
        this.properties = properties;
    }

    @Override
    public DataPermissionDecision decide(DataPermissionRequest request, AuthPrincipal principal) {
        if (principal == null) {
            return DataPermissionDecision.deny();
        }
        boolean admin = principal.roles().stream()
                .anyMatch(role -> role.equalsIgnoreCase(properties.getAdminRole()));
        if (admin) {
            return DataPermissionDecision.all();
        }
        if (principal.id() == null) {
            return DataPermissionDecision.deny();
        }
        return DataPermissionDecision.self(principal.id());
    }
}

