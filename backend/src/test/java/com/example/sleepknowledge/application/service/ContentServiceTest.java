package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ContentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private InMemoryContentRepository contents;
    private NarrationAssetRepositoryPort narrations;
    private ContentService service;

    @BeforeEach
    void setUp() {
        contents = new InMemoryContentRepository();
        narrations = mock(NarrationAssetRepositoryPort.class);
        service = new ContentService(contents, narrations, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void 콘텐츠_생성은_자동_TTS를_예약하지_않는다() {
        Episode created = service.createContent(draft("새 이야기", "새 원고"), "quiet-writer");

        assertThat(service.getContent(created.id())).isEqualTo(created);
        assertThat(created.authorUsername()).isEqualTo("quiet-writer");
        verify(narrations, never()).resetToPending(any(), any(), any(), any(), any());
        verify(narrations, never()).deleteByContentId(any());
    }

    @Test
    void 원고가_같은_metadata_수정은_기존_audio를_보존한다() {
        Episode original = service.createContent(draft("첫 제목", "같은 원고"), "first-author");

        Episode updated = service.updateContent(original.id(), draft("수정 제목", "같은 원고"));

        assertThat(updated.title()).isEqualTo("수정 제목");
        assertThat(updated.authorUsername()).isEqualTo("first-author");
        verify(narrations, never()).deleteByContentId(original.id());
        verify(narrations, never()).resetToPending(any(), any(), any(), any(), any());
    }

    @Test
    void 원고가_바뀌면_MP3를_즉시_지우고_자동_TTS는_호출하지_않는다() {
        Episode original = service.createContent(draft("첫 제목", "이전 원고"), "first-author");

        service.updateContent(original.id(), draft("수정 제목", "변경된 원고"));

        verify(narrations).deleteByContentId(original.id());
        verify(narrations, never()).resetToPending(any(), any(), any(), any(), any());
    }

    @Test
    void 없는_콘텐츠는_명확한_예외를_던진다() {
        UUID missingId = UUID.randomUUID();
        assertThatThrownBy(() -> service.getContent(missingId))
                .isInstanceOf(ContentNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    private EpisodeDraft draft(String title, String script) {
        return new EpisodeDraft(title, "편안한 요약", ContentCategory.SCIENCE, script);
    }

    private static final class InMemoryContentRepository implements ContentRepositoryPort {
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
        public Optional<Episode> findByIdForUpdate(UUID contentId) {
            return findById(contentId);
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
