package com.example.sleepknowledge.domain.model;

import java.util.Objects;

/** TTS 공급자와 무관하게 애플리케이션이 사용하는 음성 모델입니다. */
public record Voice(String id, String name, String description, String locale) {

    public Voice {
        id = requireText(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNullElse(description, "");
        locale = Objects.requireNonNullElse(locale, "");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
