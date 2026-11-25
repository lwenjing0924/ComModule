package com.commodule.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter extends OncePerRequestFilter {

    private final WebProperties properties;

    public TraceIdFilter(WebProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String incomingTraceId = request.getHeader(properties.getTraceIdHeader());
        String traceId = TraceContext.isValidTraceId(incomingTraceId, properties.getMaxTraceIdLength())
                ? incomingTraceId
                : UUID.randomUUID().toString().replace("-", "");

        MDC.put(TraceContext.TRACE_ID_KEY, traceId);
        response.setHeader(properties.getTraceIdHeader(), traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.TRACE_ID_KEY);
        }
    }
}

