package com.commodule.codegen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class CodeGeneratorValidator {

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    private static final Pattern CLASS_PATTERN = Pattern.compile("[A-Z][A-Za-z0-9]*");
    private static final Pattern FIELD_PATTERN = Pattern.compile("[a-z][A-Za-z0-9]*");
    private static final Pattern TABLE_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "String",
            "Long",
            "Integer",
            "Boolean",
            "BigDecimal",
            "LocalDate",
            "LocalDateTime",
            "UUID",
            "Double",
            "Float",
            "Short",
            "Byte"
    );

    private CodeGeneratorValidator() {
    }

    public static void validate(GenerationRequest request) {
        if (request == null) {
            throw new CodeGenerationException("生成请求不能为空");
        }
        requireMatch(request.basePackage(), PACKAGE_PATTERN, "basePackage");
        requireMatch(request.className(), CLASS_PATTERN, "className");
        requireMatch(request.tableName(), TABLE_PATTERN, "tableName");
        Set<String> fieldNames = new HashSet<>();
        for (FieldDefinition field : request.fields()) {
            if (field == null) {
                throw new CodeGenerationException("字段定义不能为空");
            }
            requireMatch(field.name(), FIELD_PATTERN, "field.name");
            if (field.name().equals("id")) {
                throw new CodeGenerationException("id 字段由生成器自动添加，请勿重复声明");
            }
            if (!fieldNames.add(field.name())) {
                throw new CodeGenerationException("字段名重复: " + field.name());
            }
            if (!SUPPORTED_TYPES.contains(field.javaType())) {
                throw new CodeGenerationException("不支持的 Java 类型: " + field.javaType());
            }
        }
    }

    private static void requireMatch(String value, Pattern pattern, String fieldName) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new CodeGenerationException("非法的 " + fieldName + ": " + value);
        }
    }

    public static String sqlType(String javaType) {
        return switch (javaType) {
            case "String", "UUID" -> javaType.equals("UUID") ? "VARCHAR(36)" : "VARCHAR(255)";
            case "Long" -> "BIGINT";
            case "Integer" -> "INTEGER";
            case "Boolean" -> "BOOLEAN";
            case "BigDecimal" -> "DECIMAL(19,2)";
            case "LocalDate" -> "DATE";
            case "LocalDateTime" -> "TIMESTAMP";
            case "Double" -> "DOUBLE";
            case "Float" -> "FLOAT";
            case "Short", "Byte" -> "SMALLINT";
            default -> throw new CodeGenerationException("不支持的 Java 类型: " + javaType);
        };
    }
}

