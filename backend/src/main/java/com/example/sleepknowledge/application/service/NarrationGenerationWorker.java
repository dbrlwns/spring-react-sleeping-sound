package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.SpeechSynthesisException;
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
import java.util.Locale;

/** 외부 TTS 호출 동안 DB transaction을 열어 두지 않는 백그라운드 worker입니다. */
@Service
public class NarrationGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(NarrationGenerationWorker.class);

    private final ContentRepositoryPort contentRepository;
    private final NarrationAssetRepositoryPort narrationRepository;
    private final SpeechSynthesisPort speechSynthesisPort;
    private final Clock clock;

    public NarrationGenerationWorker(
            ContentRepositoryPort contentRepository,
            NarrationAssetRepositoryPort narrationRepository,
            SpeechSynthesisPort speechSynthesisPort,
            Clock clock
    ) {
        this.contentRepository = contentRepository;
        this.narrationRepository = narrationRepository;
        this.speechSynthesisPort = speechSynthesisPort;
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
            if (!episode.updatedAt().equals(request.sourceUpdatedAt())) {
                throw new IllegalStateException("원고가 변경되어 이전 내레이션 결과를 폐기합니다.");
            }

            Voice voice = selectDefaultVoice(speechSynthesisPort.findAvailableVoices());
            NarrationOptions options = new NarrationOptions(voice.id(), NarrationDefaults.SPEED);
            AudioContent audio = speechSynthesisPort.synthesize(episode.script(), options, voice);
            boolean stored = narrationRepository.markReady(
                    request.contentId(),
                    request.generationId(),
                    voice,
                    options,
                    audio,
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

    static Voice selectDefaultVoice(List<Voice> voices) {
        if (voices == null || voices.isEmpty()) {
            throw new SpeechSynthesisException("사용 가능한 TTS 음성이 없습니다.");
        }

        return voices.stream()
                .filter(NarrationGenerationWorker::isKoreanVoice)
                .findFirst()
                .orElse(voices.get(0));
    }

    private static boolean isKoreanVoice(Voice voice) {
        return voice.locale().toLowerCase(Locale.ROOT).startsWith("ko");
    }
}
