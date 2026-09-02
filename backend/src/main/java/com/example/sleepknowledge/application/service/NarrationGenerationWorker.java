package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.SpeechSynthesisException;
import com.example.sleepknowledge.application.port.out.AudioTranscodingPort;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationDefaults;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/** 외부 TTS 호출 동안 DB transaction을 열어 두지 않는 백그라운드 worker입니다. */
@Service
public class NarrationGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(NarrationGenerationWorker.class);

    private final ContentRepositoryPort contentRepository;
    private final NarrationAssetRepositoryPort narrationRepository;
    private final SpeechSynthesisPort speechSynthesisPort;
    private final AudioTranscodingPort audioTranscodingPort;
    private final Clock clock;

    public NarrationGenerationWorker(
            ContentRepositoryPort contentRepository,
            NarrationAssetRepositoryPort narrationRepository,
            SpeechSynthesisPort speechSynthesisPort,
            AudioTranscodingPort audioTranscodingPort,
            Clock clock
    ) {
        this.contentRepository = contentRepository;
        this.narrationRepository = narrationRepository;
        this.speechSynthesisPort = speechSynthesisPort;
        this.audioTranscodingPort = audioTranscodingPort;
        this.clock = clock;
    }

    public void generate(NarrationGenerationRequested request) {
        if (!narrationRepository.markProcessing(
                request.contentId(),
                request.generationId(),
                clock.instant()
        )) {
            return;
        }

        try {
            Episode episode = contentRepository.findById(request.contentId())
                    .orElseThrow(() -> new ContentNotFoundException(request.contentId()));
            boolean stillCurrent = narrationRepository.findState(request.contentId())
                    .map(state -> state.generationId().equals(request.generationId()))
                    .orElse(false);
            if (!stillCurrent) {
                // 원고 수정으로 generation row가 제거/교체된 경우 비용이 드는 공급자 호출 전에 중단합니다.
                return;
            }
            // 로컬 FFmpeg 문제를 먼저 발견해 비용이 드는 Cloud TTS 호출을 피합니다.
            audioTranscodingPort.verifyAvailable();
            Voice voice = selectRequestedVoice(
                    speechSynthesisPort.findAvailableVoices(),
                    request.voiceId()
            );
            NarrationOptions options = new NarrationOptions(voice.id(), NarrationDefaults.SPEED);
            AudioContent linear16Wav = speechSynthesisPort.synthesize(episode.script(), options, voice);
            AudioContent mp3 = audioTranscodingPort.encodeMp3(linear16Wav);
            boolean stored = narrationRepository.markReady(
                    request.contentId(),
                    request.generationId(),
                    voice,
                    options,
                    mp3,
                    clock.instant()
            );
            if (!stored) {
                log.debug("Discarded stale narration generation {} for content {}",
                        request.generationId(), request.contentId());
            }
        } catch (RuntimeException exception) {
            boolean stored = narrationRepository.markFailed(
                    request.contentId(),
                    request.generationId(),
                    exception.getMessage(),
                    clock.instant()
            );
            if (stored) {
                log.warn("Automatic narration generation failed for content {}", request.contentId(), exception);
            } else {
                log.debug("Ignored failure from stale narration generation {} for content {}",
                        request.generationId(), request.contentId());
            }
        }
    }

    static Voice selectRequestedVoice(List<Voice> voices, String voiceId) {
        if (voices == null || voices.isEmpty()) {
            throw new SpeechSynthesisException("사용 가능한 TTS 음성이 없습니다.");
        }
        return voices.stream()
                .filter(voice -> voice.id().equals(voiceId))
                .findFirst()
                .orElseThrow(() -> new SpeechSynthesisException("승인된 TTS 음성을 사용할 수 없습니다."));
    }
}
