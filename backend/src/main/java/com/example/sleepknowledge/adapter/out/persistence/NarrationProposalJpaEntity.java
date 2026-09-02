package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "narration_proposals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_narration_proposal_open_content",
                columnNames = "open_content_id"
        )
)
class NarrationProposalJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID contentId;

    /** PENDING일 때만 값이 있어 DB unique constraint로 동시 중복 제안을 막습니다. */
    @Column(name = "open_content_id")
    private UUID openContentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NarrationProposalStatus status;

    @Column(nullable = false, length = 50)
    private String requestedBy;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt;

    /** 기존 제안 row와 무본문 API 요청을 위해 nullable로 둡니다. */
    @Column(name = "preferred_voice_id", length = 100)
    private String preferredVoiceId;

    @Column(length = 100)
    private String selectedVoiceId;

    @Column(length = 50)
    private String decidedBy;

    private Instant decidedAt;

    protected NarrationProposalJpaEntity() {
        // JPA 전용 생성자입니다.
    }

    private NarrationProposalJpaEntity(NarrationProposal proposal) {
        this.id = proposal.id();
        this.contentId = proposal.contentId();
        this.openContentId = proposal.contentId();
        this.status = proposal.status();
        this.requestedBy = proposal.requestedBy();
        this.requestedAt = proposal.requestedAt();
        this.preferredVoiceId = proposal.preferredVoiceId();
    }

    static NarrationProposalJpaEntity pending(NarrationProposal proposal) {
        return new NarrationProposalJpaEntity(proposal);
    }

    boolean isPending() {
        return status == NarrationProposalStatus.PENDING;
    }

    void approve(String voiceId, String administrator, Instant now) {
        this.selectedVoiceId = NarrationVoiceOption.fromVoiceId(voiceId).voiceId();
        this.status = NarrationProposalStatus.APPROVED;
        this.decidedBy = administrator;
        this.decidedAt = now;
        this.openContentId = null;
    }

    void reject(String administrator, Instant now) {
        this.status = NarrationProposalStatus.REJECTED;
        this.selectedVoiceId = null;
        this.decidedBy = administrator;
        this.decidedAt = now;
        this.openContentId = null;
    }

    NarrationProposal toDomain() {
        return new NarrationProposal(
                id,
                contentId,
                status,
                requestedBy,
                requestedAt,
                preferredVoiceId,
                selectedVoiceId,
                decidedBy,
                decidedAt
        );
    }
}
