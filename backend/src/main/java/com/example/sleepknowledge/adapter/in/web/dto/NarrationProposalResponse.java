package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;

import java.time.Instant;
import java.util.UUID;

/** 공개 상세에서는 계정 식별자를 제외하고 제안 처리 상태만 보여 줍니다. */
public record NarrationProposalResponse(
        UUID id,
        UUID contentId,
        NarrationProposalStatus status,
        Instant requestedAt,
        String preferredVoiceId,
        String selectedVoiceId,
        Instant decidedAt
) {

    public static NarrationProposalResponse from(NarrationProposal proposal) {
        return new NarrationProposalResponse(
                proposal.id(),
                proposal.contentId(),
                proposal.status(),
                proposal.requestedAt(),
                proposal.preferredVoiceId(),
                proposal.selectedVoiceId(),
                proposal.decidedAt()
        );
    }
}
