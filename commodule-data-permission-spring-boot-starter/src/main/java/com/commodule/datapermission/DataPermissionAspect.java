package com.commodule.datapermission;

import com.commodule.auth.AuthPrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;

@Aspect
@Order
public class DataPermissionAspect {

    private final DataPermissionRule rule;
    private final DataFieldReader fieldReader;

    public DataPermissionAspect(DataPermissionRule rule, DataFieldReader fieldReader) {
        this.rule = rule;
        this.fieldReader = fieldReader;
    }

    @Around("@annotation(com.commodule.datapermission.DataPermission)"
            + " || @within(com.commodule.datapermission.DataPermission)")
    public Object apply(ProceedingJoinPoint joinPoint) throws Throwable {
        DataPermission annotation = findAnnotation(joinPoint);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        AuthPrincipal principal = currentPrincipal();
        DataPermissionRequest request = new DataPermissionRequest(
                annotation.resource(),
                joinPoint.getSignature().toShortString(),
                annotation.ownerField()
        );
        DataPermissionDecision decision = rule.decide(request, principal);
        try (DataPermissionContext.Scope ignored =
                     DataPermissionContext.open(annotation.resource(), decision)) {
            Object result = joinPoint.proceed();
            if (!annotation.filterResult() || decision.scope() == DataScope.ALL) {
                return result;
            }
            return filterResult(result, annotation, decision);
        }
    }

    private Object filterResult(
            Object result,
            DataPermission annotation,
            DataPermissionDecision decision
    ) {
        if (result == null) {
            return null;
        }
        if (result instanceof DataPage<?> page) {
            List<?> filtered = filterList(page.records(), annotation, decision);
            return new DataPage<>(filtered, filtered.size());
        }
        if (result instanceof List<?> list) {
            return filterList(list, annotation, decision);
        }
        throw new DataPermissionException(
                "数据权限自动过滤仅支持 List 或 DataPage，当前返回类型: " + result.getClass().getName()
        );
    }

    private List<?> filterList(
            List<?> records,
            DataPermission annotation,
            DataPermissionDecision decision
    ) {
        return records.stream()
                .filter(record -> decision.allows(fieldReader.read(record, annotation.ownerField())))
                .toList();
    }

    private DataPermission findAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataPermission annotation = AnnotatedElementUtils.findMergedAnnotation(method, DataPermission.class);
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), DataPermission.class);
    }

    private AuthPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return null;
        }
        return principal;
    }
}

