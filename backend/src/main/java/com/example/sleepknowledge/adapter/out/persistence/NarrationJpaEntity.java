package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationDefaults;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.example.sleepknowledge.domain.model.Voice;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "narration_assets")
class NarrationJpaEntity {

    @Id
    private UUID contentId;

    @Column(nullable = false)
    private UUID generationId;

    @Column(nullable = false)
    private Instant sourceUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NarrationStatus status;

    @Column(length = 100)
    private String voiceId;

    @Column(nullable = false)
    private double speed;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] audioBytes;

    @Column(length = 100)
    private String mediaType;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private boolean audioAvailable;

    @Column(nullable = false)
    private Instant updatedAt;

    protected NarrationJpaEntity() {
        // JPA 전용 생성자입니다.
    }

    private NarrationJpaEntity(
            UUID contentId,
            UUID generationId,
            Instant sourceUpdatedAt,
            String voiceId,
            Instant updatedAt
    ) {
        this.contentId = contentId;
        this.generationId = generationId;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.status = NarrationStatus.PENDING;
        this.voiceId = NarrationVoiceOption.fromVoiceId(voiceId).voiceId();
        this.speed = NarrationDefaults.SPEED;
        this.audioAvailable = false;
        this.updatedAt = updatedAt;
    }

    static NarrationJpaEntity pending(
            UUID contentId,
            UUID generationId,
            Instant sourceUpdatedAt,
            String voiceId,
            Instant updatedAt
    ) {
        return new NarrationJpaEntity(contentId, generationId, sourceUpdatedAt, voiceId, updatedAt);
    }

    boolean matchesProcessingGeneration(UUID expectedGenerationId) {
        return generationId.equals(expectedGenerationId) && status == NarrationStatus.PROCESSING;
    }

    void complete(Voice voice, NarrationOptions options, AudioContent audio, Instant completedAt) {
        if (!AudioContent.MP3_MEDIA_TYPE.equals(audio.mediaType())) {
            throw new IllegalArgumentException("only audio/mpeg narration assets can be persisted");
        }
        this.status = NarrationStatus.READY;
        this.voiceId = voice.id();
        this.speed = options.speed();
        this.audioBytes = audio.bytes();
        this.mediaType = audio.mediaType();
        this.errorMessage = null;
        this.audioAvailable = true;
        this.updatedAt = completedAt;
    }
}
