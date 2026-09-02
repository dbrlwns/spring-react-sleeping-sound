package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationDefaults;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.Voice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaNarrationAssetRepositoryAdapter implements NarrationAssetRepositoryPort {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final SpringDataNarrationRepository repository;

    public JpaNarrationAssetRepositoryAdapter(SpringDataNarrationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public NarrationState resetToPending(
            UUID contentId,
            UUID generationId,
            Instant sourceUpdatedAt,
            String voiceId,
            Instant updatedAt
    ) {
        int resetCount = repository.resetExisting(
                contentId,
                generationId,
                sourceUpdatedAt,
                voiceId,
                NarrationStatus.PENDING,
                NarrationDefaults.SPEED,
                updatedAt
        );
        if (resetCount == 0) {
            repository.save(NarrationJpaEntity.pending(
                    contentId, generationId, sourceUpdatedAt, voiceId, updatedAt
            ));
        }
        return pendingState(contentId, generationId, sourceUpdatedAt, voiceId, updatedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NarrationState> findState(UUID contentId) {
        return repository.findStateProjection(contentId).map(projection -> {
            if (projection.getSelectedVoiceId() == null || projection.getSelectedVoiceId().isBlank()) {
                // 이전 자동 생성 버전에서 voice 선택 전 멈춘 row는 새 흐름의 미요청 상태로 읽습니다.
                return NarrationState.notRequested(
                        projection.getContentId(),
                        projection.getSourceUpdatedAt()
                );
            }
            return new NarrationState(
                    projection.getContentId(),
                    projection.getGenerationId(),
                    projection.getSourceUpdatedAt(),
                    projection.getStatus(),
                    projection.getSelectedVoiceId(),
                    projection.getErrorMessage(),
                    projection.getUpdatedAt(),
                    projection.getAudioAvailable()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AudioContent> findReadyAudio(UUID contentId) {
        return repository.findReadyAudioProjection(
                        contentId,
                        NarrationStatus.READY,
                        AudioContent.MP3_MEDIA_TYPE
                )
                .map(projection -> {
                    if (!AudioContent.MP3_MEDIA_TYPE.equals(projection.getMediaType())) {
                        throw new IllegalStateException("ready narration is not MP3");
                    }
                    return AudioContent.mp3(projection.getAudioBytes());
                });
    }

    @Override
    @Transactional
    public void deleteByContentId(UUID contentId) {
        repository.deleteById(contentId);
    }

    @Override
    @Transactional
    public boolean markProcessing(UUID contentId, UUID generationId, Instant updatedAt) {
        return repository.markProcessing(
                contentId,
                generationId,
                NarrationStatus.PENDING,
                NarrationStatus.PROCESSING,
                updatedAt
        ) == 1;
    }

    @Override
    @Transactional
    public boolean markReady(
            UUID contentId,
            UUID generationId,
            Voice voice,
            NarrationOptions options,
            AudioContent audio,
            Instant updatedAt
    ) {
        if (!AudioContent.MP3_MEDIA_TYPE.equals(audio.mediaType())) {
            throw new IllegalArgumentException("only audio/mpeg narration assets can be persisted");
        }
        Optional<NarrationJpaEntity> entity = repository.findByIdForUpdate(contentId);
        if (entity.isEmpty() || !entity.get().matchesProcessingGeneration(generationId)) {
            return false;
        }
        entity.get().complete(voice, options, audio, updatedAt);
        return true;
    }

    @Override
    @Transactional
    public boolean markFailed(UUID contentId, UUID generationId, String errorMessage, Instant updatedAt) {
        return repository.markFailed(
                contentId,
                generationId,
                NarrationStatus.PROCESSING,
                NarrationStatus.FAILED,
                normalizeErrorMessage(errorMessage),
                updatedAt
        ) == 1;
    }

    private NarrationState pendingState(
            UUID contentId,
            UUID generationId,
            Instant sourceUpdatedAt,
            String voiceId,
            Instant updatedAt
    ) {
        return new NarrationState(
                contentId,
                generationId,
                sourceUpdatedAt,
                NarrationStatus.PENDING,
                voiceId,
                null,
                updatedAt,
                false
        );
    }

    private String normalizeErrorMessage(String errorMessage) {
        String normalized = errorMessage == null || errorMessage.isBlank()
                ? "내레이션 생성에 실패했습니다."
                : errorMessage.trim();
        return normalized.substring(0, Math.min(normalized.length(), MAX_ERROR_MESSAGE_LENGTH));
    }
}
