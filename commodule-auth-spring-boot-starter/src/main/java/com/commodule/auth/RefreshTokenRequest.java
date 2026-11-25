package com.commodule.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "不能为空") String refreshToken
) {
}
