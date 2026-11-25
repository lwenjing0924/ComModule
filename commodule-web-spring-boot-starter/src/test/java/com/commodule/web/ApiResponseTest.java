package com.commodule.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void includesCurrentTraceId() {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(TraceContext.TRACE_ID_KEY, "trace-test")) {
            ApiResponse<String> response = ApiResponse.success("data");

            assertThat(response.code()).isZero();
            assertThat(response.traceId()).isEqualTo("trace-test");
            assertThat(response.data()).isEqualTo("data");
        }
    }
}

