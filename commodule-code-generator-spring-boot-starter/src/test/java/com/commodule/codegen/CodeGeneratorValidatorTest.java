package com.commodule.codegen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGeneratorValidatorTest {

    @Test
    void rejectsInvalidClassName() {
        GenerationRequest request = new GenerationRequest(
                "com.example",
                "user-profile",
                "user_profile",
                "",
                List.of()
        );

        assertThatThrownBy(() -> CodeGeneratorValidator.validate(request))
                .isInstanceOf(CodeGenerationException.class);
    }

    @Test
    void rejectsUnsupportedTypeAndDuplicateField() {
        GenerationRequest unsupported = new GenerationRequest(
                "com.example",
                "User",
                "user",
                "",
                List.of(new FieldDefinition("nickname", "Object", false))
        );
        GenerationRequest duplicate = new GenerationRequest(
                "com.example",
                "User",
                "user",
                "",
                List.of(
                        new FieldDefinition("nickname", "String", false),
                        new FieldDefinition("nickname", "String", false)
                )
        );

        assertThatThrownBy(() -> CodeGeneratorValidator.validate(unsupported))
                .isInstanceOf(CodeGenerationException.class);
        assertThatThrownBy(() -> CodeGeneratorValidator.validate(duplicate))
                .isInstanceOf(CodeGenerationException.class);
    }

    @Test
    void mapsSqlTypes() {
        assertThat(CodeGeneratorValidator.sqlType("Long")).isEqualTo("BIGINT");
        assertThat(CodeGeneratorValidator.sqlType("BigDecimal")).isEqualTo("DECIMAL(19,2)");
        assertThat(CodeGeneratorValidator.sqlType("UUID")).isEqualTo("VARCHAR(36)");
    }
}

