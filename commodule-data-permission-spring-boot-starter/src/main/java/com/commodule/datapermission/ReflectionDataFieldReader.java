package com.commodule.datapermission;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionDataFieldReader implements DataFieldReader {

    private final Map<FieldKey, Accessor> accessors = new ConcurrentHashMap<>();

    @Override
    public Object read(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new DataPermissionException("数据权限字段名不能为空");
        }
        try {
            return accessors
                    .computeIfAbsent(new FieldKey(target.getClass(), fieldName), this::resolve)
                    .read(target);
        } catch (ReflectiveOperationException exception) {
            throw new DataPermissionException(
                    "无法读取数据权限字段: " + target.getClass().getName() + "." + fieldName,
                    exception
            );
        }
    }

    private Accessor resolve(FieldKey key) {
        Method method = findAccessor(key.type(), key.fieldName());
        if (method != null) {
            return target -> method.invoke(target);
        }
        Field field = findField(key.type(), key.fieldName());
        if (field != null) {
            if (!field.trySetAccessible()) {
                throw new DataPermissionException("无法访问数据权限字段: " + key);
            }
            return target -> field.get(target);
        }
        throw new DataPermissionException("找不到数据权限字段: " + key.type().getName() + "." + key.fieldName());
    }

    private Method findAccessor(Class<?> type, String fieldName) {
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (String candidate : new String[]{fieldName, "get" + suffix, "is" + suffix}) {
            try {
                Method method = type.getMethod(candidate);
                if (method.getParameterCount() == 0 && method.getReturnType() != Void.TYPE) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private record FieldKey(Class<?> type, String fieldName) {
    }

    @FunctionalInterface
    private interface Accessor {

        Object read(Object target) throws ReflectiveOperationException;
    }
}

