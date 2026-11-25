package com.commodule.codegen;

import java.util.Locale;

public final class Naming {

    private Naming() {
    }

    public static String camelToSnake(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isUpperCase(current) && index > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(current));
        }
        return result.toString();
    }

    public static String packagePath(String basePackage) {
        return basePackage.replace('.', '/');
    }

    public static String lowerFirst(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static String normalizedTableName(String tableName) {
        return tableName.toLowerCase(Locale.ROOT);
    }
}

