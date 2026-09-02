package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.authentication.UserRole;
import org.springframework.security.core.Authentication;

public record AuthSessionResponse(boolean authenticated, String username, UserRole role) {

    public static AuthSessionResponse authenticated(Authentication authentication) {
        UserRole role = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))
                ? UserRole.ADMIN
                : UserRole.USER;
        return new AuthSessionResponse(true, authentication.getName(), role);
    }

    public static AuthSessionResponse anonymous() {
        return new AuthSessionResponse(false, null, null);
    }
}
