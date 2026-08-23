package com.example.sleepknowledge.adapter.in.web.dto;

public record AuthSessionResponse(boolean authenticated, String username) {

    public static AuthSessionResponse authenticated(String username) {
        return new AuthSessionResponse(true, username);
    }

    public static AuthSessionResponse anonymous() {
        return new AuthSessionResponse(false, null);
    }
}
