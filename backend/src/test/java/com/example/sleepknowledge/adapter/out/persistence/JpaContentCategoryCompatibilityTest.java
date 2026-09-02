package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-content-category-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JpaContentCategoryCompatibilityTest {

    @MockitoBean
    private TextToSpeechClient textToSpeechClient;

    private final ContentRepositoryPort repository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JpaContentCategoryCompatibilityTest(ContentRepositoryPort repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void 기존_H2_과학_분류를_JPA로_계속_저장하고_읽는다() {
        UUID contentId = UUID.randomUUID();
        Instant savedAt = Instant.parse("2026-08-23T08:00:00Z");
        repository.save(new Episode(
                contentId,
                "기존 우주론 콘텐츠",
                "이전 DB 분류 호환을 확인합니다.",
                ContentCategory.COSMOLOGY,
                "기존 우주론 원고입니다.",
                savedAt,
                savedAt
        ));

        assertThat(repository.findById(contentId).orElseThrow().category())
                .isEqualTo(ContentCategory.COSMOLOGY);
    }

    @Test
    void 작성자_컬럼이_null인_기존_H2_row는_안전한_이름으로_읽는다() {
        UUID contentId = UUID.randomUUID();
        Instant savedAt = Instant.parse("2026-08-24T08:00:00Z");
        jdbcTemplate.update(
                """
                insert into contents (
                    id, title, summary, category, script, author_username, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                contentId,
                "작성자 컬럼 이전 콘텐츠",
                "nullable migration 호환을 확인합니다.",
                ContentCategory.SCIENCE.name(),
                "기존 데이터베이스 원고입니다.",
                null,
                savedAt,
                savedAt
        );

        Episode legacy = repository.findById(contentId).orElseThrow();

        assertThat(legacy.authorUsername()).isEqualTo(Episode.LEGACY_AUTHOR_USERNAME);
    }
}
