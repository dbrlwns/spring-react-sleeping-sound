package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.NarrationNotReadyException;
import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.port.in.BrowseNarrationUseCase;
import com.example.sleepknowledge.application.port.in.ListNarrationVoicesUseCase;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.Voice;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** TTS는 저장된 지식 콘텐츠를 전달하기 위한 보조 기능으로만 조율합니다. */
@Service
@Transactional(readOnly = true)
public class NarrationService implements BrowseNarrationUseCase, ListNarrationVoicesUseCase {

    private final ContentRepositoryPort contentRepository;
    private final NarrationAssetRepositoryPort narrationRepository;
    private final SpeechSynthesisPort speechSynthesisPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public NarrationService(
            ContentRepositoryPort contentRepository,
            NarrationAssetRepositoryPort narrationRepository,
            SpeechSynthesisPort speechSynthesisPort,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.contentRepository = contentRepository;
        this.narrationRepository = narrationRepository;
        this.speechSynthesisPort = speechSynthesisPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public List<Voice> listVoices() {
        return speechSynthesisPort.findAvailableVoices();
    }

    @Override
    @Transactional
    public NarrationState getNarrationStatus(UUID contentId) {
        Episode episode = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        return ensureCurrentGeneration(episode);
    }

    @Override
    @Transactional(noRollbackFor = NarrationNotReadyException.class)
    public AudioContent getNarrationAudio(UUID contentId) {
        Episode episode = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        NarrationState state = ensureCurrentGeneration(episode);
        if (state.status() != NarrationStatus.READY) {
            throw new NarrationNotReadyException(contentId, state.status());
        }

        return narrationRepository.findReadyAudio(contentId)
                .orElseThrow(() -> new NarrationNotReadyException(contentId, state.status()));
    }

    private NarrationState ensureCurrentGeneration(Episode episode) {
        Optional<NarrationState> existing = narrationRepository.findState(episode.id());
        NarrationState state;
        if (existing.isEmpty() || !existing.get().sourceUpdatedAt().equals(episode.updatedAt())) {
            state = narrationRepository.resetToPending(
                    episode.id(),
                    UUID.randomUUID(),
                    episode.updatedAt(),
                    clock.instant()
            );
        } else {
            state = existing.get();
        }

        if (state.status() == NarrationStatus.PENDING) {
            eventPublisher.publishEvent(new NarrationGenerationRequested(
                    state.contentId(),
                    state.generationId(),
                    state.sourceUpdatedAt()
            ));
        }

        return state;
    }
}
