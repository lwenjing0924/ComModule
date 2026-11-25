package com.commodule.web;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(0, "OK", HttpStatus.OK),
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未认证或凭证已失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权访问该资源", HttpStatus.FORBIDDEN),
    CORS_REJECTED(40301, "跨域请求被拒绝", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(40500, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(40900, "资源状态冲突", HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE(41500, "请求内容类型不支持", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INTERNAL_ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}

