package com.example.sleepknowledge.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 상태 조회에 필요한 메타데이터이며 WAV 본문은 포함하지 않습니다. */
public record NarrationState(
        UUID contentId,
        UUID generationId,
        Instant sourceUpdatedAt,
        NarrationStatus status,
        String errorMessage,
        Instant updatedAt,
        boolean audioAvailable
) {

    public NarrationState {
        Objects.requireNonNull(contentId, "contentId must not be null");
        Objects.requireNonNull(generationId, "generationId must not be null");
        Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        errorMessage = errorMessage == null || errorMessage.isBlank() ? null : errorMessage.trim();

        if (audioAvailable != (status == NarrationStatus.READY)) {
            throw new IllegalStateException("audio is available only when narration is ready");
        }
    }
}
