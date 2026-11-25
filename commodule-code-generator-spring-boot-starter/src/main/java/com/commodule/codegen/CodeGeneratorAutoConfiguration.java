package com.commodule.codegen;

import com.commodule.codegen.template.DomainTemplate;
import com.commodule.codegen.template.RequestTemplate;
import com.commodule.codegen.template.ServiceTemplate;
import com.commodule.codegen.template.SqlTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.util.List;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = "commodule.code-generator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(CodeGeneratorProperties.class)
public class CodeGeneratorAutoConfiguration {

    @Bean
    @Order(10)
    public CodeTemplate domainTemplate() {
        return new DomainTemplate();
    }

    @Bean
    @Order(20)
    public CodeTemplate requestTemplate() {
        return new RequestTemplate();
    }

    @Bean
    @Order(30)
    public CodeTemplate serviceTemplate() {
        return new ServiceTemplate();
    }

    @Bean
    @Order(40)
    public CodeTemplate sqlTemplate() {
        return new SqlTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public CodeGenerator codeGenerator(List<CodeTemplate> templates) {
        return new TemplateCodeGenerator(templates);
    }
}

