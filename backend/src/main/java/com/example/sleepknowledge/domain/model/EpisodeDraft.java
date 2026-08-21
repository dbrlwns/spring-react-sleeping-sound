package com.example.sleepknowledge.domain.model;

import java.util.Objects;

/** 새 에피소드를 만들거나 기존 에피소드를 수정할 때 사용하는 값입니다. */
public record EpisodeDraft(String title, String summary, ContentCategory category, String script) {

    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_SUMMARY_LENGTH = 500;
    public static final int MAX_SCRIPT_LENGTH = 20_000;

    public EpisodeDraft {
        title = requireText(title, "title", MAX_TITLE_LENGTH);
        summary = requireText(summary, "summary", MAX_SUMMARY_LENGTH);
        category = Objects.requireNonNull(category, "category must not be null");
        script = requireText(script, "script", MAX_SCRIPT_LENGTH);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return trimmed;
    }
}
