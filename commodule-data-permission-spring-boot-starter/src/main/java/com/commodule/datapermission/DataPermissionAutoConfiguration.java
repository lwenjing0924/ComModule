package com.commodule.datapermission;

import com.commodule.auth.AuthAutoConfiguration;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = AuthAutoConfiguration.class)
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(
        prefix = "commodule.data-permission",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(DataPermissionProperties.class)
public class DataPermissionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataFieldReader reflectionDataFieldReader() {
        return new ReflectionDataFieldReader();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataPermissionRule roleBasedDataPermissionRule(DataPermissionProperties properties) {
        return new RoleBasedDataPermissionRule(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataPermissionAspect dataPermissionAspect(
            DataPermissionRule rule,
            DataFieldReader fieldReader
    ) {
        return new DataPermissionAspect(rule, fieldReader);
    }
}

