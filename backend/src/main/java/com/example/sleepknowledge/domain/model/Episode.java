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
        Instant createdAt,
        Instant updatedAt
) {

    public Episode {
        Objects.requireNonNull(id, "id must not be null");
        EpisodeDraft validated = new EpisodeDraft(title, summary, category, script);
        title = validated.title();
        summary = validated.summary();
        category = validated.category();
        script = validated.script();
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public static Episode create(UUID id, EpisodeDraft draft, Instant now) {
        Objects.requireNonNull(draft, "draft must not be null");
        return new Episode(
                id,
                draft.title(),
                draft.summary(),
                draft.category(),
                draft.script(),
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
                createdAt,
                now
        );
    }
}
