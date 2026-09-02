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
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
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

    public NarrationService(
            ContentRepositoryPort contentRepository,
            NarrationAssetRepositoryPort narrationRepository
    ) {
        this.contentRepository = contentRepository;
        this.narrationRepository = narrationRepository;
    }

    @Override
    public List<Voice> listVoices() {
        return NarrationVoiceOption.voices();
    }

    @Override
    public NarrationState getNarrationStatus(UUID contentId) {
        Episode episode = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        return currentState(episode);
    }

    @Override
    public AudioContent getNarrationAudio(UUID contentId) {
        Episode episode = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        NarrationState state = currentState(episode);
        if (state.status() != NarrationStatus.READY) {
            throw new NarrationNotReadyException(contentId, state.status());
        }

        return narrationRepository.findReadyAudio(contentId)
                .orElseThrow(() -> new NarrationNotReadyException(contentId, state.status()));
    }

    private NarrationState currentState(Episode episode) {
        return narrationRepository.findState(episode.id())
                .orElseGet(() -> NarrationState.notRequested(episode.id(), episode.updatedAt()));
    }
}
