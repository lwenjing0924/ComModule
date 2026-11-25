package com.commodule.codegen.template;

import com.commodule.codegen.CodeTemplate;
import com.commodule.codegen.FieldDefinition;
import com.commodule.codegen.GeneratedFile;
import com.commodule.codegen.GenerationRequest;
import com.commodule.codegen.Naming;

import java.util.List;

public class DomainTemplate implements CodeTemplate {

    @Override
    public String name() {
        return "domain";
    }

    @Override
    public List<GeneratedFile> render(GenerationRequest request) {
        StringBuilder source = new StringBuilder();
        source.append("package ").append(request.basePackage()).append(".domain;\n\n");
        source.append("public record ").append(request.className()).append("(\n");
        source.append("        Long id");
        for (FieldDefinition field : request.fields()) {
            source.append(",\n        ").append(field.javaType()).append(" ").append(field.name());
        }
        source.append("\n) {\n}\n");
        String path = "src/main/java/" + Naming.packagePath(request.basePackage())
                + "/domain/" + request.className() + ".java";
        return List.of(new GeneratedFile(path, source.toString()));
    }
}

