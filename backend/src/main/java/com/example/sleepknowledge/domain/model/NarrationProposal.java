package com.example.sleepknowledge.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** TTS 결과와 별도로 보존하는 사용자 제안 및 관리자 결정 기록입니다. */
public record NarrationProposal(
        UUID id,
        UUID contentId,
        NarrationProposalStatus status,
        String requestedBy,
        Instant requestedAt,
        String preferredVoiceId,
        String selectedVoiceId,
        String decidedBy,
        Instant decidedAt
) {

    public NarrationProposal {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(contentId, "contentId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        requestedBy = requireText(requestedBy, "requestedBy");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        // 과거의 무본문 제안 row는 null일 수 있지만, 값이 있다면 서버 허용 목록만 보존합니다.
        if (preferredVoiceId != null) {
            preferredVoiceId = NarrationVoiceOption.fromVoiceId(preferredVoiceId).voiceId();
        }

        if (status == NarrationProposalStatus.PENDING) {
            if (selectedVoiceId != null || decidedBy != null || decidedAt != null) {
                throw new IllegalArgumentException("pending proposal must not contain a decision");
            }
        } else {
            decidedBy = requireText(decidedBy, "decidedBy");
            Objects.requireNonNull(decidedAt, "decidedAt must not be null");
            if (decidedAt.isBefore(requestedAt)) {
                throw new IllegalArgumentException("decidedAt must not be before requestedAt");
            }
            if (status == NarrationProposalStatus.APPROVED) {
                selectedVoiceId = NarrationVoiceOption.fromVoiceId(selectedVoiceId).voiceId();
            } else if (selectedVoiceId != null) {
                throw new IllegalArgumentException("rejected proposal must not select a voice");
            }
        }
    }

    public static NarrationProposal pending(
            UUID id,
            UUID contentId,
            String requestedBy,
            Instant requestedAt,
            String preferredVoiceId
    ) {
        return new NarrationProposal(
                id, contentId, NarrationProposalStatus.PENDING, requestedBy, requestedAt,
                preferredVoiceId, null, null, null
        );
    }

    /** 무본문 POST를 사용하던 이전 클라이언트와 저장 데이터의 호환용 생성 메서드입니다. */
    public static NarrationProposal pending(
            UUID id,
            UUID contentId,
            String requestedBy,
            Instant requestedAt
    ) {
        return pending(id, contentId, requestedBy, requestedAt, null);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
