package com.commodule.auth;

import java.util.Set;

public record MeResponse(Long id, String username, Set<String> roles) {
}

