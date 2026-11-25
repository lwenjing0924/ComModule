package com.commodule.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    @Test
    void keepsValidIncomingTraceId() throws Exception {
        WebProperties properties = new WebProperties();
        TraceIdFilter filter = new TraceIdFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-123");
    }

    @Test
    void replacesInvalidTraceId() throws Exception {
        WebProperties properties = new WebProperties();
        TraceIdFilter filter = new TraceIdFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "bad trace id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Trace-Id"))
                .isNotBlank()
                .doesNotContain(" ");
    }
}

