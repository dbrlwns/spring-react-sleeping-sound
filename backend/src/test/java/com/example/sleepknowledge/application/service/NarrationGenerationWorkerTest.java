package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationDefaults;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.Voice;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NarrationGenerationWorkerTest {

    private static final Instant SOURCE_UPDATED_AT = Instant.parse("2026-08-20T11:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private final UUID contentId = UUID.randomUUID();
    private final Episode episode = new Episode(
            contentId,
            "별 이야기",
            "요약",
            ContentCategory.ASTRONOMY,
            "자동으로 읽을 저장 원고입니다.",
            SOURCE_UPDATED_AT,
            SOURCE_UPDATED_AT
    );
    private final InMemoryNarrationRepository narrationRepository = new InMemoryNarrationRepository();
    private final FakeSpeechPort speechPort = new FakeSpeechPort();
    private final NarrationGenerationWorker worker = new NarrationGenerationWorker(
            new SingleContentRepository(episode),
            narrationRepository,
            speechPort,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void 공급자의_첫_한국어_음성과_기본_속도_0_9로_저장된_원고를_합성한다() {
        Voice english = new Voice("Samantha", "Samantha", "", "en_US");
        Voice korean = new Voice("Sora", "Sora", "", "ko_KR");
        Voice yuna = new Voice("Yuna", "Yuna", "", "ko_KR");
        speechPort.voices = List.of(english, korean, yuna);
        UUID generationId = prepareGeneration();

        worker.generate(request(generationId));

        assertThat(narrationRepository.state.status()).isEqualTo(NarrationStatus.READY);
        assertThat(narrationRepository.findReadyAudio(contentId).orElseThrow().bytes())
                .containsExactly(82, 73, 70, 70);
        assertThat(speechPort.lastScript).isEqualTo(episode.script());
        assertThat(speechPort.lastVoice).isEqualTo(korean);
        assertThat(speechPort.lastOptions.speed()).isEqualTo(NarrationDefaults.SPEED);
        assertThat(speechPort.lastOptions.voiceId()).isEqualTo("Sora");
    }

    @Test
    void 한국어_음성을_선택한다() {
        Voice english = new Voice("Alex", "Alex", "", "en_US");
        Voice korean = new Voice("Sora", "Sora", "", "ko_KR");

        assertThat(NarrationGenerationWorker.selectDefaultVoice(List.of(english, korean)))
                .isEqualTo(korean);
    }

    @Test
    void 한국어가_없으면_공급자의_첫_음성을_선택한다() {
        Voice first = new Voice("Alex", "Alex", "", "en_US");
        Voice second = new Voice("Alice", "Alice", "", "it_IT");

        assertThat(NarrationGenerationWorker.selectDefaultVoice(List.of(first, second)))
                .isEqualTo(first);
    }

    @Test
    void 합성_실패는_failed_상태와_오류로_저장한다() {
        speechPort.failure = new IllegalStateException("provider unavailable");
        UUID generationId = prepareGeneration();

        worker.generate(request(generationId));

        assertThat(narrationRepository.state.status()).isEqualTo(NarrationStatus.FAILED);
        assertThat(narrationRepository.state.errorMessage()).isEqualTo("provider unavailable");
        assertThat(narrationRepository.findReadyAudio(contentId)).isEmpty();
    }

    @Test
    void 합성_도중_새_세대로_바뀌면_오래된_wav가_덮어쓰지_못한다() {
        UUID oldGeneration = prepareGeneration();
        UUID newGeneration = UUID.randomUUID();
        speechPort.beforeReturn = () -> narrationRepository.resetToPending(
                contentId,
                newGeneration,
                SOURCE_UPDATED_AT.plusSeconds(1),
                NOW.plusSeconds(1)
        );

        worker.generate(request(oldGeneration));

        assertThat(narrationRepository.state.generationId()).isEqualTo(newGeneration);
        assertThat(narrationRepository.state.status()).isEqualTo(NarrationStatus.PENDING);
        assertThat(narrationRepository.findReadyAudio(contentId)).isEmpty();
    }

    @Test
    void 이미_교체된_세대의_이벤트는_TTS를_시작하지_않는다() {
        UUID currentGeneration = prepareGeneration();

        worker.generate(request(UUID.randomUUID()));

        assertThat(narrationRepository.state.generationId()).isEqualTo(currentGeneration);
        assertThat(narrationRepository.state.status()).isEqualTo(NarrationStatus.PENDING);
        assertThat(speechPort.synthesizeCount).isZero();
    }

    private UUID prepareGeneration() {
        UUID generationId = UUID.randomUUID();
        narrationRepository.resetToPending(contentId, generationId, SOURCE_UPDATED_AT, NOW);
        return generationId;
    }

    private NarrationGenerationRequested request(UUID generationId) {
        return new NarrationGenerationRequested(contentId, generationId, SOURCE_UPDATED_AT);
    }

    private final class FakeSpeechPort implements SpeechSynthesisPort {
        private List<Voice> voices = List.of(new Voice("Yuna", "Yuna", "", "ko_KR"));
        private RuntimeException failure;
        private Runnable beforeReturn = () -> { };
        private String lastScript;
        private NarrationOptions lastOptions;
        private Voice lastVoice;
        private int synthesizeCount;

        @Override
        public List<Voice> findAvailableVoices() {
            return voices;
        }

        @Override
        public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
            synthesizeCount++;
            lastScript = script;
            lastOptions = options;
            lastVoice = voice;
            if (failure != null) {
                throw failure;
            }
            beforeReturn.run();
            return AudioContent.wav(new byte[]{82, 73, 70, 70});
        }
    }

    private static final class InMemoryNarrationRepository implements NarrationAssetRepositoryPort {
        private NarrationState state;
        private AudioContent audio;

        @Override
        public synchronized NarrationState resetToPending(
                UUID contentId,
                UUID generationId,
                Instant sourceUpdatedAt,
                Instant updatedAt
        ) {
            state = new NarrationState(
                    contentId,
                    generationId,
                    sourceUpdatedAt,
                    NarrationStatus.PENDING,
                    null,
                    updatedAt,
                    false
            );
            audio = null;
            return state;
        }

        @Override
        public synchronized Optional<NarrationState> findState(UUID contentId) {
            return state != null && state.contentId().equals(contentId) ? Optional.of(state) : Optional.empty();
        }

        @Override
        public synchronized Optional<AudioContent> findReadyAudio(UUID contentId) {
            return state != null && state.contentId().equals(contentId) ? Optional.ofNullable(audio) : Optional.empty();
        }

        @Override
        public synchronized boolean markProcessing(UUID contentId, UUID generationId, Instant updatedAt) {
            if (!matches(contentId, generationId, NarrationStatus.PENDING)) {
                return false;
            }
            state = new NarrationState(
                    contentId,
                    generationId,
                    state.sourceUpdatedAt(),
                    NarrationStatus.PROCESSING,
                    null,
                    updatedAt,
                    false
            );
            return true;
        }

        @Override
        public synchronized boolean markReady(
                UUID contentId,
                UUID generationId,
                Voice voice,
                NarrationOptions options,
                AudioContent readyAudio,
                Instant updatedAt
        ) {
            if (!matches(contentId, generationId, NarrationStatus.PROCESSING)) {
                return false;
            }
            audio = readyAudio;
            state = new NarrationState(
                    contentId,
                    generationId,
                    state.sourceUpdatedAt(),
                    NarrationStatus.READY,
                    null,
                    updatedAt,
                    true
            );
            return true;
        }

        @Override
        public synchronized boolean markFailed(
                UUID contentId,
                UUID generationId,
                String errorMessage,
                Instant updatedAt
        ) {
            if (!matches(contentId, generationId, NarrationStatus.PROCESSING)) {
                return false;
            }
            audio = null;
            state = new NarrationState(
                    contentId,
                    generationId,
                    state.sourceUpdatedAt(),
                    NarrationStatus.FAILED,
                    errorMessage,
                    updatedAt,
                    false
            );
            return true;
        }

        private boolean matches(UUID contentId, UUID generationId, NarrationStatus expectedStatus) {
            return state != null
                    && state.contentId().equals(contentId)
                    && state.generationId().equals(generationId)
                    && state.status() == expectedStatus;
        }
    }

    private record SingleContentRepository(Episode episode) implements ContentRepositoryPort {
        @Override
        public List<Episode> findAll() {
            return List.of(episode);
        }

        @Override
        public Optional<Episode> findById(UUID requestedId) {
            return episode.id().equals(requestedId) ? Optional.of(episode) : Optional.empty();
        }

        @Override
        public Episode save(Episode saved) {
            return saved;
        }

        @Override
        public long count() {
            return 1;
        }
    }
}
