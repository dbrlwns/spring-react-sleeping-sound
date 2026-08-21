package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private final FakeContentRepository repository = new FakeContentRepository();
    private final ContentService service = new ContentService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void 콘텐츠를_생성하고_목록과_상세에서_조회한다() {
        Episode created = service.createContent(draft("양자 이야기"));

        assertThat(created.id()).isNotNull();
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
        assertThat(service.listContents()).containsExactly(created);
        assertThat(service.getContent(created.id())).isEqualTo(created);
    }

    @Test
    void 콘텐츠를_수정하면_생성시각은_보존한다() {
        Episode original = service.createContent(draft("첫 제목"));

        Episode updated = service.updateContent(original.id(), draft("수정된 제목"));

        assertThat(updated.title()).isEqualTo("수정된 제목");
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void 없는_콘텐츠는_명확한_예외를_던진다() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> service.getContent(missingId))
                .isInstanceOf(ContentNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    private EpisodeDraft draft(String title) {
        return new EpisodeDraft(
                title,
                "편안한 요약",
                ContentCategory.QUANTUM_PHYSICS,
                "잠들기 전에 천천히 들어볼 원고입니다."
        );
    }

    private static final class FakeContentRepository implements ContentRepositoryPort {
        private final Map<UUID, Episode> episodes = new LinkedHashMap<>();

        @Override
        public List<Episode> findAll() {
            return new ArrayList<>(episodes.values());
        }

        @Override
        public Optional<Episode> findById(UUID contentId) {
            return Optional.ofNullable(episodes.get(contentId));
        }

        @Override
        public Episode save(Episode episode) {
            episodes.put(episode.id(), episode);
            return episode;
        }

        @Override
        public long count() {
            return episodes.size();
        }
    }
}
