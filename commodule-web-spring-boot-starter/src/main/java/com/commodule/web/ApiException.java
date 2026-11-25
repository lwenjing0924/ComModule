package com.commodule.web;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public ApiException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.message(), errorCode.status());
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode.code(), message, errorCode.status());
    }

    public ApiException(int code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(ErrorCode.BAD_REQUEST, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(ErrorCode.UNAUTHORIZED, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(ErrorCode.FORBIDDEN, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, message);
    }
}

