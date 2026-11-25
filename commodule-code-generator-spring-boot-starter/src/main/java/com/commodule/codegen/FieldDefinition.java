package com.commodule.codegen;

public record FieldDefinition(String name, String javaType, boolean required) {

    public FieldDefinition {
        javaType = javaType == null || javaType.isBlank() ? "String" : javaType;
    }

    public String columnName() {
        return Naming.camelToSnake(name);
    }
}

