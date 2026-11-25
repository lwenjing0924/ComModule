package com.commodule.codegen;

import com.commodule.codegen.template.DomainTemplate;
import com.commodule.codegen.template.RequestTemplate;
import com.commodule.codegen.template.ServiceTemplate;
import com.commodule.codegen.template.SqlTemplate;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateCodeGeneratorTest {

    private final TemplateCodeGenerator generator = new TemplateCodeGenerator(List.of(
            new DomainTemplate(),
            new RequestTemplate(),
            new ServiceTemplate(),
            new SqlTemplate()
    ));

    @Test
    void generatesDefaultFiles() {
        GenerationResult result = generator.generate(request());

        assertThat(result.files()).hasSize(4);
        assertThat(result.files())
                .extracting(GeneratedFile::path)
                .contains(
                        "src/main/java/com/example/demo/domain/UserProfile.java",
                        "src/main/java/com/example/demo/web/UserProfileRequest.java",
                        "src/main/java/com/example/demo/service/UserProfileService.java",
                        "src/main/resources/db/user_profile.sql"
                );

        String domain = result.files().stream()
                .filter(file -> file.path().endsWith("UserProfile.java"))
                .findFirst()
                .orElseThrow()
                .content();
        assertThat(domain).contains("public record UserProfile(", "Long id", "String nickname");

        String sql = result.files().stream()
                .filter(file -> file.path().endsWith("user_profile.sql"))
                .findFirst()
                .orElseThrow()
                .content();
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS user_profile", "nickname VARCHAR(255) NOT NULL");
    }

    @Test
    void createsZipArchive() throws Exception {
        byte[] zip = generator.generate(request()).toZip();

        int entries = 0;
        try (ZipInputStream input = new ZipInputStream(
                new java.io.ByteArrayInputStream(zip),
                StandardCharsets.UTF_8
        )) {
            while (input.getNextEntry() != null) {
                entries++;
            }
        }

        assertThat(zip).isNotEmpty();
        assertThat(entries).isEqualTo(4);
    }

    @Test
    void rejectsDuplicateOutputPaths() {
        CodeTemplate first = template("first");
        CodeTemplate second = template("second");
        TemplateCodeGenerator duplicateGenerator = new TemplateCodeGenerator(List.of(first, second));

        assertThatThrownBy(() -> duplicateGenerator.generate(request()))
                .isInstanceOf(CodeGenerationException.class);
    }

    private GenerationRequest request() {
        return new GenerationRequest(
                "com.example.demo",
                "UserProfile",
                "user_profile",
                "用户资料",
                List.of(
                        new FieldDefinition("nickname", "String", true),
                        new FieldDefinition("age", "Integer", false)
                )
        );
    }

    private CodeTemplate template(String name) {
        return new CodeTemplate() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public List<GeneratedFile> render(GenerationRequest request) {
                return List.of(new GeneratedFile("same/path.txt", name));
            }
        };
    }
}

