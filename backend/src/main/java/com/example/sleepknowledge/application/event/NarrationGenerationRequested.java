package com.example.sleepknowledge.application.event;

import com.example.sleepknowledge.domain.model.NarrationVoiceOption;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 콘텐츠 트랜잭션이 commit된 뒤 실행할 내레이션 세대를 식별합니다. */
public record NarrationGenerationRequested(
        UUID contentId,
        UUID generationId,
        Instant sourceUpdatedAt,
        String voiceId
) {

    public NarrationGenerationRequested {
        Objects.requireNonNull(contentId, "contentId must not be null");
        Objects.requireNonNull(generationId, "generationId must not be null");
        Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt must not be null");
        voiceId = NarrationVoiceOption.fromVoiceId(voiceId).voiceId();
    }
}
