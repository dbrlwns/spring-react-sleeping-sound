package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.exception.NarrationProposalAlreadyPendingException;
import com.example.sleepknowledge.application.exception.NarrationProposalConflictException;
import com.example.sleepknowledge.application.exception.NarrationProposalNotFoundException;
import com.example.sleepknowledge.application.port.out.NarrationProposalRepositoryPort;
import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaNarrationProposalRepositoryAdapter implements NarrationProposalRepositoryPort {

    private final SpringDataNarrationProposalRepository repository;

    public JpaNarrationProposalRepositoryAdapter(SpringDataNarrationProposalRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public NarrationProposal createPending(NarrationProposal proposal) {
        try {
            // flush까지 수행해야 unique 위반을 서비스 트랜잭션 안에서 409로 변환할 수 있습니다.
            return repository.saveAndFlush(NarrationProposalJpaEntity.pending(proposal)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new NarrationProposalAlreadyPendingException(proposal.contentId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NarrationProposal> findLatestByContentId(UUID contentId) {
        Optional<NarrationProposalJpaEntity> open = repository
                .findFirstByContentIdAndStatusOrderByRequestedAtDesc(
                        contentId,
                        NarrationProposalStatus.PENDING
                );
        return open.or(() -> repository.findFirstByContentIdOrderByRequestedAtDesc(contentId))
                .map(NarrationProposalJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NarrationProposal> findAllByStatus(NarrationProposalStatus status) {
        return repository.findAllByStatusOrderByRequestedAtAsc(status).stream()
                .map(NarrationProposalJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public NarrationProposal approvePending(
            UUID proposalId,
            String voiceId,
            String decidedBy,
            Instant decidedAt
    ) {
        NarrationProposalJpaEntity entity = pendingForUpdate(proposalId);
        entity.approve(voiceId, decidedBy, decidedAt);
        return entity.toDomain();
    }

    @Override
    @Transactional
    public NarrationProposal rejectPending(UUID proposalId, String decidedBy, Instant decidedAt) {
        NarrationProposalJpaEntity entity = pendingForUpdate(proposalId);
        entity.reject(decidedBy, decidedAt);
        return entity.toDomain();
    }

    private NarrationProposalJpaEntity pendingForUpdate(UUID proposalId) {
        NarrationProposalJpaEntity entity = repository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new NarrationProposalNotFoundException(proposalId));
        if (!entity.isPending()) {
            throw new NarrationProposalConflictException();
        }
        return entity;
    }
}
