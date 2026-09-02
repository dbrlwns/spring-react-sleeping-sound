package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.domain.model.NarrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SpringDataNarrationRepository extends JpaRepository<NarrationJpaEntity, UUID> {

    @Query("""
            select n.contentId as contentId,
                   n.generationId as generationId,
                   n.sourceUpdatedAt as sourceUpdatedAt,
                   n.status as status,
                   n.voiceId as selectedVoiceId,
                   n.errorMessage as errorMessage,
                   n.updatedAt as updatedAt,
                   n.audioAvailable as audioAvailable
              from NarrationJpaEntity n
             where n.contentId = :contentId
            """)
    Optional<NarrationStateProjection> findStateProjection(@Param("contentId") UUID contentId);

    @Query("""
            select n.audioBytes as audioBytes,
                   n.mediaType as mediaType
              from NarrationJpaEntity n
             where n.contentId = :contentId
               and n.status = :readyStatus
               and n.audioAvailable = true
               and n.mediaType = :mediaType
            """)
    Optional<NarrationAudioProjection> findReadyAudioProjection(
            @Param("contentId") UUID contentId,
            @Param("readyStatus") NarrationStatus readyStatus,
            @Param("mediaType") String mediaType
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NarrationJpaEntity n
               set n.generationId = :generationId,
                   n.sourceUpdatedAt = :sourceUpdatedAt,
                   n.status = :pendingStatus,
                   n.voiceId = :voiceId,
                   n.speed = :speed,
                   n.audioBytes = null,
                   n.mediaType = null,
                   n.errorMessage = null,
                   n.audioAvailable = false,
                   n.updatedAt = :updatedAt
             where n.contentId = :contentId
            """)
    int resetExisting(
            @Param("contentId") UUID contentId,
            @Param("generationId") UUID generationId,
            @Param("sourceUpdatedAt") Instant sourceUpdatedAt,
            @Param("voiceId") String voiceId,
            @Param("pendingStatus") NarrationStatus pendingStatus,
            @Param("speed") double speed,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NarrationJpaEntity n
               set n.status = :processingStatus,
                   n.errorMessage = null,
                   n.updatedAt = :updatedAt
             where n.contentId = :contentId
               and n.generationId = :generationId
               and n.status = :pendingStatus
            """)
    int markProcessing(
            @Param("contentId") UUID contentId,
            @Param("generationId") UUID generationId,
            @Param("pendingStatus") NarrationStatus pendingStatus,
            @Param("processingStatus") NarrationStatus processingStatus,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NarrationJpaEntity n
               set n.status = :failedStatus,
                   n.audioBytes = null,
                   n.mediaType = null,
                   n.errorMessage = :errorMessage,
                   n.audioAvailable = false,
                   n.updatedAt = :updatedAt
             where n.contentId = :contentId
               and n.generationId = :generationId
               and n.status = :processingStatus
            """)
    int markFailed(
            @Param("contentId") UUID contentId,
            @Param("generationId") UUID generationId,
            @Param("processingStatus") NarrationStatus processingStatus,
            @Param("failedStatus") NarrationStatus failedStatus,
            @Param("errorMessage") String errorMessage,
            @Param("updatedAt") Instant updatedAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NarrationJpaEntity n where n.contentId = :contentId")
    Optional<NarrationJpaEntity> findByIdForUpdate(@Param("contentId") UUID contentId);

    interface NarrationStateProjection {
        UUID getContentId();

        UUID getGenerationId();

        Instant getSourceUpdatedAt();

        NarrationStatus getStatus();

        String getSelectedVoiceId();

        String getErrorMessage();

        Instant getUpdatedAt();

        boolean getAudioAvailable();
    }

    interface NarrationAudioProjection {
        byte[] getAudioBytes();

        String getMediaType();
    }
}
