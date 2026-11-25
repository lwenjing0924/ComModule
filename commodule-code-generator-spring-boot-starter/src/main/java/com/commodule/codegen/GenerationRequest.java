package com.commodule.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record GenerationRequest(
        String basePackage,
        String className,
        String tableName,
        String description,
        List<FieldDefinition> fields
) {

    public GenerationRequest {
        fields = fields == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(fields));
        if ((tableName == null || tableName.isBlank()) && className != null) {
            tableName = Naming.camelToSnake(className);
        }
    }
}

