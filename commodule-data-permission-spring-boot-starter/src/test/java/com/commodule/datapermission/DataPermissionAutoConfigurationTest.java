package com.commodule.datapermission;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DataPermissionAutoConfigurationTest {

    @Test
    void providesDefaultComponents() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataPermissionAutoConfiguration.class))
                .run(context -> {
                    assertThat(context.getBean(DataFieldReader.class))
                            .isInstanceOf(ReflectionDataFieldReader.class);
                    assertThat(context.getBean(DataPermissionRule.class))
                            .isInstanceOf(RoleBasedDataPermissionRule.class);
                    assertThat(context.getBean(DataPermissionAspect.class)).isNotNull();
                });
    }

    @Test
    void canBeDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataPermissionAutoConfiguration.class))
                .withPropertyValues("commodule.data-permission.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DataPermissionAspect.class));
    }
}

