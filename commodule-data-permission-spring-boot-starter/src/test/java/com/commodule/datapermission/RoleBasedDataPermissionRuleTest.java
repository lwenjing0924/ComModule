package com.commodule.datapermission;

import com.commodule.auth.AuthPrincipal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoleBasedDataPermissionRuleTest {

    private final DataPermissionProperties properties = new DataPermissionProperties();
    private final RoleBasedDataPermissionRule rule = new RoleBasedDataPermissionRule(properties);
    private final DataPermissionRequest request = new DataPermissionRequest("order", "listOrders", "ownerId");

    @Test
    void adminCanSeeAllData() {
        AuthPrincipal principal = new AuthPrincipal(1L, "admin", Set.of("ADMIN"));

        DataPermissionDecision decision = rule.decide(request, principal);

        assertThat(decision.scope()).isEqualTo(DataScope.ALL);
        assertThat(decision.allows(999L)).isTrue();
    }

    @Test
    void regularUserCanOnlySeeOwnData() {
        AuthPrincipal principal = new AuthPrincipal(9L, "alice", Set.of("USER"));

        DataPermissionDecision decision = rule.decide(request, principal);

        assertThat(decision.scope()).isEqualTo(DataScope.SELF);
        assertThat(decision.allows(9L)).isTrue();
        assertThat(decision.allows("9")).isTrue();
        assertThat(decision.allows(10L)).isFalse();
    }

    @Test
    void missingIdentityIsDenied() {
        assertThat(rule.decide(request, null).scope()).isEqualTo(DataScope.DENY);
        assertThat(rule.decide(request, new AuthPrincipal(null, "anonymous", Set.of("USER"))).scope())
                .isEqualTo(DataScope.DENY);
    }
}

