package com.example.sleepknowledge.application.port.out;

import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 제안 저장소의 동시성 제어와 조회를 애플리케이션에 제공하는 계약입니다. */
public interface NarrationProposalRepositoryPort {

    NarrationProposal createPending(NarrationProposal proposal);

    Optional<NarrationProposal> findLatestByContentId(UUID contentId);

    List<NarrationProposal> findAllByStatus(NarrationProposalStatus status);

    NarrationProposal approvePending(UUID proposalId, String voiceId, String decidedBy, Instant decidedAt);

    NarrationProposal rejectPending(UUID proposalId, String decidedBy, Instant decidedAt);
}
