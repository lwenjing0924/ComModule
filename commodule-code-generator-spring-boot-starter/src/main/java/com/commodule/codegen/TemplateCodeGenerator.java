package com.commodule.codegen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TemplateCodeGenerator implements CodeGenerator {

    private final List<CodeTemplate> templates;

    public TemplateCodeGenerator(List<CodeTemplate> templates) {
        this.templates = List.copyOf(templates);
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        CodeGeneratorValidator.validate(request);
        List<GeneratedFile> files = templates.stream()
                .flatMap(template -> template.render(request).stream())
                .toList();
        Set<String> paths = new HashSet<>();
        for (GeneratedFile file : files) {
            if (!paths.add(file.path())) {
                throw new CodeGenerationException("生成文件路径重复: " + file.path());
            }
        }
        return new GenerationResult(files);
    }
}

