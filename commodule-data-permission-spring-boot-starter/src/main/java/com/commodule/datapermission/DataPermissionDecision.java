package com.commodule.datapermission;

import java.util.Objects;

public record DataPermissionDecision(DataScope scope, Long ownerId) {

    public DataPermissionDecision {
        scope = Objects.requireNonNull(scope, "scope");
    }

    public static DataPermissionDecision all() {
        return new DataPermissionDecision(DataScope.ALL, null);
    }

    public static DataPermissionDecision self(Long ownerId) {
        return new DataPermissionDecision(DataScope.SELF, Objects.requireNonNull(ownerId, "ownerId"));
    }

    public static DataPermissionDecision deny() {
        return new DataPermissionDecision(DataScope.DENY, null);
    }

    public boolean allows(Object candidateOwnerId) {
        return switch (scope) {
            case ALL -> true;
            case DENY -> false;
            case SELF -> matches(candidateOwnerId, ownerId);
        };
    }

    private boolean matches(Object candidate, Long expected) {
        if (candidate == null || expected == null) {
            return false;
        }
        if (candidate instanceof Number number) {
            return number.longValue() == expected;
        }
        try {
            return Long.parseLong(candidate.toString()) == expected;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}

