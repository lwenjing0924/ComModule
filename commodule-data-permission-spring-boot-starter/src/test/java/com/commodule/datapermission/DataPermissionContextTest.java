package com.commodule.datapermission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataPermissionContextTest {

    @Test
    void restoresPreviousContext() {
        assertThat(DataPermissionContext.current()).isEmpty();

        try (DataPermissionContext.Scope outer =
                     DataPermissionContext.open("order", DataPermissionDecision.all())) {
            assertThat(DataPermissionContext.current()).isPresent();
            assertThat(DataPermissionContext.current().orElseThrow().resource()).isEqualTo("order");

            try (DataPermissionContext.Scope inner =
                         DataPermissionContext.open("invoice", DataPermissionDecision.self(7L))) {
                assertThat(DataPermissionContext.current().orElseThrow().resource()).isEqualTo("invoice");
            }

            assertThat(DataPermissionContext.current().orElseThrow().resource()).isEqualTo("order");
        }

        assertThat(DataPermissionContext.current()).isEmpty();
    }
}

