package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-content-category-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JpaContentCategoryCompatibilityTest {

    private final ContentRepositoryPort repository;

    @Autowired
    JpaContentCategoryCompatibilityTest(ContentRepositoryPort repository) {
        this.repository = repository;
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
}
