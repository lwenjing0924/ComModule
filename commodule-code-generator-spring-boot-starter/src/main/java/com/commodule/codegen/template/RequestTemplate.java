package com.commodule.codegen.template;

import com.commodule.codegen.CodeTemplate;
import com.commodule.codegen.FieldDefinition;
import com.commodule.codegen.GeneratedFile;
import com.commodule.codegen.GenerationRequest;
import com.commodule.codegen.Naming;

import java.util.ArrayList;
import java.util.List;

public class RequestTemplate implements CodeTemplate {

    @Override
    public String name() {
        return "request";
    }

    @Override
    public List<GeneratedFile> render(GenerationRequest request) {
        StringBuilder source = new StringBuilder();
        source.append("package ").append(request.basePackage()).append(".web;\n\n");
        source.append("public record ").append(request.className()).append("Request(\n");
        List<String> fields = new ArrayList<>();
        for (FieldDefinition field : request.fields()) {
            fields.add("        " + field.javaType() + " " + field.name());
        }
        source.append(String.join(",\n", fields));
        if (!fields.isEmpty()) {
            source.append("\n");
        }
        source.append(") {\n}\n");
        String path = "src/main/java/" + Naming.packagePath(request.basePackage())
                + "/web/" + request.className() + "Request.java";
        return List.of(new GeneratedFile(path, source.toString()));
    }
}

