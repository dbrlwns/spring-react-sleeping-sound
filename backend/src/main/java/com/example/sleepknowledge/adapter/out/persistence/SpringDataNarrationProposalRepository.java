package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataNarrationProposalRepository
        extends JpaRepository<NarrationProposalJpaEntity, UUID> {

    Optional<NarrationProposalJpaEntity> findFirstByContentIdOrderByRequestedAtDesc(UUID contentId);

    Optional<NarrationProposalJpaEntity> findFirstByContentIdAndStatusOrderByRequestedAtDesc(
            UUID contentId,
            NarrationProposalStatus status
    );

    List<NarrationProposalJpaEntity> findAllByStatusOrderByRequestedAtAsc(NarrationProposalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from NarrationProposalJpaEntity p where p.id = :proposalId")
    Optional<NarrationProposalJpaEntity> findByIdForUpdate(@Param("proposalId") UUID proposalId);
}
