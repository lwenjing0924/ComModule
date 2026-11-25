package com.commodule.web;

import java.time.Instant;

public record ApiResponse<T>(int code, String message, T data, String traceId, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.code(),
                ErrorCode.SUCCESS.message(),
                data,
                TraceContext.currentTraceId(),
                Instant.now()
        );
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, TraceContext.currentTraceId(), Instant.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode.code(), errorCode.message());
    }
}

