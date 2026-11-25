package com.commodule.auth;

import com.commodule.web.ApiResponse;
import com.commodule.web.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.DefaultCorsProcessor;

import java.io.IOException;

public class ApiCorsProcessor extends DefaultCorsProcessor {

    private final ObjectMapper objectMapper;

    public ApiCorsProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(ApiResponse.error(ErrorCode.CORS_REJECTED));
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setContentLength(body.length);
        response.getBody().write(body);
        response.flush();
    }
}
