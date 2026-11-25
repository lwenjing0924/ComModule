package com.commodule.web;

import org.slf4j.MDC;

public final class TraceContext {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_PATTERN = "[A-Za-z0-9._-]+";

    private TraceContext() {
    }

    public static String currentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static boolean isValidTraceId(String traceId, int maxLength) {
        return traceId != null
                && !traceId.isBlank()
                && traceId.length() <= maxLength
                && traceId.matches(TRACE_ID_PATTERN);
    }
}

