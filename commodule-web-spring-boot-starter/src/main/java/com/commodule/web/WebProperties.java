package com.commodule.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commodule.web")
public class WebProperties {

    private String traceIdHeader = "X-Trace-Id";
    private int maxTraceIdLength = 128;

    public String getTraceIdHeader() {
        return traceIdHeader;
    }

    public void setTraceIdHeader(String traceIdHeader) {
        this.traceIdHeader = traceIdHeader;
    }

    public int getMaxTraceIdLength() {
        return maxTraceIdLength;
    }

    public void setMaxTraceIdLength(int maxTraceIdLength) {
        this.maxTraceIdLength = maxTraceIdLength;
    }
}

