package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.UnsupportedVoiceException;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NarrationServiceTest {

    private final UUID contentId = UUID.randomUUID();
    private final Episode episode = new Episode(
            contentId,
            "우주 이야기",
            "요약",
            ContentCategory.COSMOLOGY,
            "저장된 원고입니다.",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")
    );
    private final Voice yuna = new Voice("Yuna", "Yuna", "한국어", "ko_KR");
    private final FakeSpeechSynthesisPort speechPort = new FakeSpeechSynthesisPort();
    private final NarrationService service = new NarrationService(
            new SingleContentRepository(episode),
            speechPort
    );

    @Test
    void 요청_본문이_아니라_저장된_콘텐츠_원고를_합성한다() {
        NarrationOptions options = new NarrationOptions("Yuna", 0.9);

        AudioContent result = service.generateNarration(contentId, options);

        assertThat(result.bytes()).containsExactly(1, 2, 3);
        assertThat(speechPort.lastScript).isEqualTo("저장된 원고입니다.");
        assertThat(speechPort.lastOptions).isEqualTo(options);
        assertThat(speechPort.lastVoice).isEqualTo(yuna);
    }

    @Test
    void 지원하지_않는_음성은_거절한다() {
        assertThatThrownBy(() -> service.generateNarration(
                contentId,
                new NarrationOptions("Unknown", 1.0)
        )).isInstanceOf(UnsupportedVoiceException.class);
    }

    @Test
    void 없는_콘텐츠는_합성하지_않는다() {
        assertThatThrownBy(() -> service.generateNarration(
                UUID.randomUUID(),
                new NarrationOptions("Yuna", 1.0)
        )).isInstanceOf(ContentNotFoundException.class);
    }

    private final class FakeSpeechSynthesisPort implements SpeechSynthesisPort {
        private String lastScript;
        private NarrationOptions lastOptions;
        private Voice lastVoice;

        @Override
        public List<Voice> findAvailableVoices() {
            return List.of(yuna);
        }

        @Override
        public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
            lastScript = script;
            lastOptions = options;
            lastVoice = voice;
            return AudioContent.wav(new byte[]{1, 2, 3});
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
