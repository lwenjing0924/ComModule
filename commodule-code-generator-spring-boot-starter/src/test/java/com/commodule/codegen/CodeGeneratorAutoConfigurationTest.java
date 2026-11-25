package com.commodule.codegen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CodeGeneratorAutoConfigurationTest {

    @Test
    void providesGeneratorAndDefaultTemplates() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CodeGeneratorAutoConfiguration.class))
                .run(context -> {
                    assertThat(context.getBean(CodeGenerator.class)).isInstanceOf(TemplateCodeGenerator.class);
                    assertThat(context.getBeansOfType(CodeTemplate.class)).hasSize(4);
                });
    }

    @Test
    void customTemplateIsAdded() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CodeGeneratorAutoConfiguration.class))
                .withBean(CodeTemplate.class, () -> new CodeTemplate() {
                    @Override
                    public String name() {
                        return "readme";
                    }

                    @Override
                    public java.util.List<GeneratedFile> render(GenerationRequest request) {
                        return java.util.List.of(new GeneratedFile("README.generated.md", "# " + request.className()));
                    }
                })
                .run(context -> {
                    assertThat(context.getBeansOfType(CodeTemplate.class)).hasSize(5);
                    CodeGenerator generator = context.getBean(CodeGenerator.class);
                    GenerationResult result = generator.generate(new GenerationRequest(
                            "com.example",
                            "User",
                            "user",
                            "",
                            java.util.List.of()
                    ));
                    assertThat(result.files())
                            .extracting(GeneratedFile::path)
                            .contains("README.generated.md");
                });
    }

    @Test
    void canBeDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CodeGeneratorAutoConfiguration.class))
                .withPropertyValues("commodule.code-generator.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CodeGenerator.class));
    }
}

