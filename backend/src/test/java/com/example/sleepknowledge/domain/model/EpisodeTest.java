package com.example.sleepknowledge.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EpisodeTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-25T00:00:00Z");

    @Test
    void 새_콘텐츠는_인증된_작성자_이름을_정규화한다() {
        Episode episode = Episode.create(
                UUID.randomUUID(),
                draft("처음 원고"),
                "  quiet-writer  ",
                CREATED_AT
        );

        assertThat(episode.authorUsername()).isEqualTo("quiet-writer");
    }

    @Test
    void 수정해도_최초_작성자를_보존한다() {
        Episode episode = Episode.create(
                UUID.randomUUID(),
                draft("처음 원고"),
                "quiet-writer",
                CREATED_AT
        );

        Episode updated = episode.update(draft("수정 원고"), CREATED_AT.plusSeconds(60));

        assertThat(updated.script()).isEqualTo("수정 원고");
        assertThat(updated.authorUsername()).isEqualTo("quiet-writer");
    }

    @Test
    void 새_콘텐츠는_작성자_없이_생성할_수_없다() {
        assertThatThrownBy(() -> Episode.create(
                UUID.randomUUID(),
                draft("원고"),
                "   ",
                CREATED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("authorUsername must not be blank for new content");
    }

    @Test
    void 작성자가_없던_legacy_콘텐츠는_안전한_fallback을_사용한다() {
        Episode legacy = new Episode(
                UUID.randomUUID(),
                "기존 이야기",
                "기존 이야기 요약",
                ContentCategory.HISTORY,
                "기존 원고",
                null,
                CREATED_AT,
                CREATED_AT
        );

        assertThat(legacy.authorUsername()).isEqualTo(Episode.LEGACY_AUTHOR_USERNAME);
    }

    private EpisodeDraft draft(String script) {
        return new EpisodeDraft("이야기", "이야기 요약", ContentCategory.SCIENCE, script);
    }
}
