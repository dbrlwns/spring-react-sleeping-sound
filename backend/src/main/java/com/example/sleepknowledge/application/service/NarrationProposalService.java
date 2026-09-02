package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.NarrationAlreadySupportedException;
import com.example.sleepknowledge.application.port.in.ReviewNarrationProposalUseCase;
import com.example.sleepknowledge.application.port.in.SuggestNarrationUseCase;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationProposalRepositoryPort;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.example.sleepknowledge.domain.model.Voice;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 사용자 제안과 관리자 결정을 조율하고, 승인된 결정만 TTS 작업으로 연결합니다. */
@Service
@Transactional(readOnly = true)
public class NarrationProposalService implements SuggestNarrationUseCase, ReviewNarrationProposalUseCase {

    private final ContentRepositoryPort contentRepository;
    private final NarrationProposalRepositoryPort proposalRepository;
    private final NarrationAssetRepositoryPort narrationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public NarrationProposalService(
            ContentRepositoryPort contentRepository,
            NarrationProposalRepositoryPort proposalRepository,
            NarrationAssetRepositoryPort narrationRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.contentRepository = contentRepository;
        this.proposalRepository = proposalRepository;
        this.narrationRepository = narrationRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public NarrationProposal suggest(UUID contentId, String requestedBy, String preferredVoiceId) {
        String validatedPreferredVoiceId = NarrationVoiceOption.fromVoiceId(preferredVoiceId).voiceId();
        requireContent(contentId);
        Optional<NarrationState> narration = narrationRepository.findState(contentId);
        if (narration.filter(state -> state.status() == NarrationStatus.PENDING
                || state.status() == NarrationStatus.PROCESSING
                || state.status() == NarrationStatus.READY).isPresent()) {
            throw new NarrationAlreadySupportedException();
        }

        return proposalRepository.createPending(NarrationProposal.pending(
                UUID.randomUUID(),
                contentId,
                requestedBy,
                clock.instant(),
                validatedPreferredVoiceId
        ));
    }

    @Override
    public Optional<NarrationProposal> findLatest(UUID contentId) {
        requireContent(contentId);
        return proposalRepository.findLatestByContentId(contentId);
    }

    @Override
    public List<NarrationProposal> list(NarrationProposalStatus status) {
        return proposalRepository.findAllByStatus(status);
    }

    @Override
    public List<Voice> listVoiceOptions() {
        return NarrationVoiceOption.voices();
    }

    @Override
    @Transactional
    public NarrationProposal approve(UUID proposalId, String voiceId, String administrator) {
        NarrationVoiceOption selectedVoice = NarrationVoiceOption.fromVoiceId(voiceId);
        Instant now = clock.instant();

        // row lock으로 PENDING 상태를 먼저 선점하므로 관리자 경합에서도 generation은 한 번만 생깁니다.
        NarrationProposal approved = proposalRepository.approvePending(
                proposalId,
                selectedVoice.voiceId(),
                administrator,
                now
        );
        Episode episode = contentRepository.findByIdForUpdate(approved.contentId())
                .orElseThrow(() -> new ContentNotFoundException(approved.contentId()));
        NarrationState state = narrationRepository.resetToPending(
                episode.id(),
                UUID.randomUUID(),
                episode.updatedAt(),
                selectedVoice.voiceId(),
                now
        );
        publishGeneration(state);
        return approved;
    }

    @Override
    @Transactional
    public NarrationProposal reject(UUID proposalId, String administrator) {
        return proposalRepository.rejectPending(proposalId, administrator, clock.instant());
    }

    private Episode requireContent(UUID contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private void publishGeneration(NarrationState state) {
        eventPublisher.publishEvent(new NarrationGenerationRequested(
                state.contentId(),
                state.generationId(),
                state.sourceUpdatedAt(),
                state.selectedVoiceId()
        ));
    }
}
