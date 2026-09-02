package com.example.sleepknowledge.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 상태 조회에 필요한 메타데이터이며 MP3 본문은 포함하지 않습니다. */
public record NarrationState(
        UUID contentId,
        UUID generationId,
        Instant sourceUpdatedAt,
        NarrationStatus status,
        String selectedVoiceId,
        String errorMessage,
        Instant updatedAt,
        boolean audioAvailable
) {

    public NarrationState {
        Objects.requireNonNull(contentId, "contentId must not be null");
        Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        errorMessage = errorMessage == null || errorMessage.isBlank() ? null : errorMessage.trim();
        selectedVoiceId = selectedVoiceId == null || selectedVoiceId.isBlank()
                ? null
                : selectedVoiceId.trim();

        if (audioAvailable != (status == NarrationStatus.READY)) {
            throw new IllegalStateException("audio is available only when narration is ready");
        }
        if (status == NarrationStatus.NOT_REQUESTED) {
            if (generationId != null || selectedVoiceId != null || errorMessage != null) {
                throw new IllegalStateException("not-requested narration must not contain generation data");
            }
        } else {
            Objects.requireNonNull(generationId, "generationId must not be null");
            if (selectedVoiceId == null) {
                throw new IllegalArgumentException("selectedVoiceId must not be blank");
            }
        }
    }

    public static NarrationState notRequested(UUID contentId, Instant sourceUpdatedAt) {
        return new NarrationState(
                contentId,
                null,
                sourceUpdatedAt,
                NarrationStatus.NOT_REQUESTED,
                null,
                null,
                sourceUpdatedAt,
                false
        );
    }
}
