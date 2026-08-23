package com.example.sleepknowledge.application.port.out;

import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.Voice;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** 비동기 내레이션 상태와 완성된 WAV를 저장하는 계약입니다. */
public interface NarrationAssetRepositoryPort {

    NarrationState resetToPending(
            UUID contentId,
            UUID generationId,
            Instant sourceUpdatedAt,
            Instant updatedAt
    );

    Optional<NarrationState> findState(UUID contentId);

    Optional<AudioContent> findReadyAudio(UUID contentId);

    boolean markProcessing(UUID contentId, UUID generationId, Instant updatedAt);

    boolean markReady(
            UUID contentId,
            UUID generationId,
            Voice voice,
            NarrationOptions options,
            AudioContent audio,
            Instant updatedAt
    );

    boolean markFailed(UUID contentId, UUID generationId, String errorMessage, Instant updatedAt);
}
