package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.UnsupportedVoiceException;
import com.example.sleepknowledge.application.port.in.GenerateNarrationUseCase;
import com.example.sleepknowledge.application.port.in.ListNarrationVoicesUseCase;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** TTS는 저장된 지식 콘텐츠를 전달하기 위한 보조 기능으로만 조율합니다. */
@Service
@Transactional(readOnly = true)
public class NarrationService implements GenerateNarrationUseCase, ListNarrationVoicesUseCase {

    private final ContentRepositoryPort contentRepository;
    private final SpeechSynthesisPort speechSynthesisPort;

    public NarrationService(
            ContentRepositoryPort contentRepository,
            SpeechSynthesisPort speechSynthesisPort
    ) {
        this.contentRepository = contentRepository;
        this.speechSynthesisPort = speechSynthesisPort;
    }

    @Override
    public List<Voice> listVoices() {
        return speechSynthesisPort.findAvailableVoices();
    }

    @Override
    public AudioContent generateNarration(UUID contentId, NarrationOptions options) {
        Episode episode = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        Voice selectedVoice = speechSynthesisPort.findAvailableVoices().stream()
                .filter(voice -> voice.id().equals(options.voiceId()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedVoiceException(options.voiceId()));

        return speechSynthesisPort.synthesize(episode.script(), options, selectedVoice);
    }
}
