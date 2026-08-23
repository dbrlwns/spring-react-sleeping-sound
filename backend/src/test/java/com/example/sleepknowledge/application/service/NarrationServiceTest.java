package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.NarrationNotReadyException;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.Voice;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NarrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private final UUID contentId = UUID.randomUUID();
    private final Episode episode = new Episode(
            contentId,
            "우주 이야기",
            "요약",
            ContentCategory.COSMOLOGY,
            "저장된 원고입니다.",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
    );
    private final Voice yuna = new Voice("Yuna", "Yuna", "한국어", "ko_KR");
    private final FakeNarrationRepository narrationRepository = new FakeNarrationRepository();
    private final List<Object> publishedEvents = new ArrayList<>();
    private final NarrationService service = new NarrationService(
            new SingleContentRepository(episode),
            narrationRepository,
            new FakeSpeechSynthesisPort(),
            publishedEvents::add,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void 기존_콘텐츠에_asset이_없으면_상태_조회가_자동_생성을_예약한다() {
        NarrationState state = service.getNarrationStatus(contentId);

        assertThat(state.status()).isEqualTo(NarrationStatus.PENDING);
        assertThat(state.sourceUpdatedAt()).isEqualTo(episode.updatedAt());
        assertThat(narrationRepository.resetCount).isEqualTo(1);
        assertThat(publishedEvents).singleElement()
                .isEqualTo(new NarrationGenerationRequested(contentId, state.generationId(), episode.updatedAt()));
    }

    @Test
    void pending_상태를_다시_조회하면_유실된_after_commit_작업을_다시_예약한다() {
        NarrationState first = service.getNarrationStatus(contentId);

        NarrationState second = service.getNarrationStatus(contentId);

        assertThat(second.generationId()).isEqualTo(first.generationId());
        assertThat(narrationRepository.resetCount).isEqualTo(1);
        assertThat(publishedEvents).hasSize(2);
    }

    @Test
    void 최신_원고와_다른_ready_asset은_즉시_무효화하고_새_세대를_예약한다() {
        UUID staleGeneration = UUID.randomUUID();
        narrationRepository.setReady(
                staleGeneration,
                episode.updatedAt().minusSeconds(1),
                AudioContent.wav(new byte[]{9, 9, 9})
        );

        NarrationState state = service.getNarrationStatus(contentId);

        assertThat(state.status()).isEqualTo(NarrationStatus.PENDING);
        assertThat(state.generationId()).isNotEqualTo(staleGeneration);
        assertThat(narrationRepository.findReadyAudio(contentId)).isEmpty();
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void ready_상태에서만_저장된_wav를_반환한다() {
        byte[] wav = {82, 73, 70, 70};
        narrationRepository.setReady(UUID.randomUUID(), episode.updatedAt(), AudioContent.wav(wav));

        AudioContent result = service.getNarrationAudio(contentId);

        assertThat(result.bytes()).containsExactly(wav);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void 준비되지_않은_audio_요청은_현재_상태와_함께_충돌_예외를_낸다() {
        assertThatThrownBy(() -> service.getNarrationAudio(contentId))
                .isInstanceOf(NarrationNotReadyException.class)
                .hasMessageContaining("PENDING");
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void 없는_콘텐츠는_상태도_조회하지_않는다() {
        assertThatThrownBy(() -> service.getNarrationStatus(UUID.randomUUID()))
                .isInstanceOf(ContentNotFoundException.class);
    }

    @Test
    void 설치된_음성_목록은_기존_API를_통해_조회할_수_있다() {
        assertThat(service.listVoices()).containsExactly(yuna);
    }

    private final class FakeSpeechSynthesisPort implements SpeechSynthesisPort {
        @Override
        public List<Voice> findAvailableVoices() {
            return List.of(yuna);
        }

        @Override
        public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
            return AudioContent.wav(new byte[]{1});
        }
    }

    private final class FakeNarrationRepository implements NarrationAssetRepositoryPort {
        private NarrationState state;
        private AudioContent audio;
        private int resetCount;

        @Override
        public NarrationState resetToPending(
                UUID requestedContentId,
                UUID generationId,
                Instant sourceUpdatedAt,
                Instant updatedAt
        ) {
            resetCount++;
            state = new NarrationState(
                    requestedContentId,
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
        public Optional<NarrationState> findState(UUID requestedContentId) {
            return state != null && state.contentId().equals(requestedContentId)
                    ? Optional.of(state)
                    : Optional.empty();
        }

        @Override
        public Optional<AudioContent> findReadyAudio(UUID requestedContentId) {
            return state != null && state.contentId().equals(requestedContentId)
                    ? Optional.ofNullable(audio)
                    : Optional.empty();
        }

        @Override
        public boolean markProcessing(UUID requestedContentId, UUID generationId, Instant updatedAt) {
            return false;
        }

        @Override
        public boolean markReady(
                UUID requestedContentId,
                UUID generationId,
                Voice voice,
                NarrationOptions options,
                AudioContent readyAudio,
                Instant updatedAt
        ) {
            return false;
        }

        @Override
        public boolean markFailed(
                UUID requestedContentId,
                UUID generationId,
                String errorMessage,
                Instant updatedAt
        ) {
            return false;
        }

        private void setReady(UUID generationId, Instant sourceUpdatedAt, AudioContent readyAudio) {
            state = new NarrationState(
                    contentId,
                    generationId,
                    sourceUpdatedAt,
                    NarrationStatus.READY,
                    null,
                    NOW,
                    true
            );
            audio = readyAudio;
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
