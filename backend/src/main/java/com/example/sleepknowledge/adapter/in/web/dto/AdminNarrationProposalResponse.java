package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminNarrationProposalResponse(
        UUID id,
        UUID contentId,
        String contentTitle,
        NarrationProposalStatus status,
        String requestedBy,
        Instant requestedAt,
        String preferredVoiceId,
        String selectedVoiceId,
        String decidedBy,
        Instant decidedAt
) {

    public static AdminNarrationProposalResponse from(NarrationProposal proposal, String contentTitle) {
        return new AdminNarrationProposalResponse(
                proposal.id(),
                proposal.contentId(),
                contentTitle,
                proposal.status(),
                proposal.requestedBy(),
                proposal.requestedAt(),
                proposal.preferredVoiceId(),
                proposal.selectedVoiceId(),
                proposal.decidedBy(),
                proposal.decidedAt()
        );
    }
}
