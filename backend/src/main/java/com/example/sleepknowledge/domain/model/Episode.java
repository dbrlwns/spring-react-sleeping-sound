package com.example.sleepknowledge.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 잠들기 전에 들을 지식 콘텐츠 한 편을 나타내는 핵심 도메인 모델입니다.
 * JPA 애노테이션을 넣지 않아 저장 기술과 분리했습니다.
 */
public record Episode(
        UUID id,
        String title,
        String summary,
        ContentCategory category,
        String script,
        String authorUsername,
        Instant createdAt,
        Instant updatedAt
) {

    /** 작성자 컬럼이 없던 시기의 콘텐츠와 예시 데이터에 표시할 안정적인 이름입니다. */
    public static final String LEGACY_AUTHOR_USERNAME = "고요한 지식";
    public static final int MAX_AUTHOR_USERNAME_LENGTH = 50;

    public Episode {
        Objects.requireNonNull(id, "id must not be null");
        EpisodeDraft validated = new EpisodeDraft(title, summary, category, script);
        title = validated.title();
        summary = validated.summary();
        category = validated.category();
        script = validated.script();
        authorUsername = normalizeAuthorUsername(authorUsername);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    /**
     * 작성자 컬럼 도입 전의 생성 코드와 seed를 위한 호환 생성자입니다.
     * 새 사용자 입력은 반드시 authorUsername을 받는 create 팩토리를 사용합니다.
     */
    public Episode(
            UUID id,
            String title,
            String summary,
            ContentCategory category,
            String script,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, title, summary, category, script, LEGACY_AUTHOR_USERNAME, createdAt, updatedAt);
    }

    public static Episode create(UUID id, EpisodeDraft draft, String authorUsername, Instant now) {
        Objects.requireNonNull(draft, "draft must not be null");
        if (authorUsername == null || authorUsername.isBlank()) {
            throw new IllegalArgumentException("authorUsername must not be blank for new content");
        }
        return new Episode(
                id,
                draft.title(),
                draft.summary(),
                draft.category(),
                draft.script(),
                authorUsername,
                now,
                now
        );
    }

    public Episode update(EpisodeDraft draft, Instant now) {
        Objects.requireNonNull(draft, "draft must not be null");
        return new Episode(
                id,
                draft.title(),
                draft.summary(),
                draft.category(),
                draft.script(),
                authorUsername,
                createdAt,
                now
        );
    }

    /** nullable legacy DB 값을 읽을 때도 API에는 항상 표시 가능한 작성자를 제공합니다. */
    public static String normalizeAuthorUsername(String authorUsername) {
        if (authorUsername == null || authorUsername.isBlank()) {
            return LEGACY_AUTHOR_USERNAME;
        }
        String normalized = authorUsername.trim();
        if (normalized.length() > MAX_AUTHOR_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "authorUsername must not exceed " + MAX_AUTHOR_USERNAME_LENGTH + " characters"
            );
        }
        return normalized;
    }
}
