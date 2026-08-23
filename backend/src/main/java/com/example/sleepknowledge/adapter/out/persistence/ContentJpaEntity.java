package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** JPA에만 필요한 매핑은 도메인 모델이 아니라 persistence adapter 안에 둡니다. */
@Entity
@Table(name = "contents")
class ContentJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ContentCategory category;

    @Lob
    @Column(nullable = false)
    private String script;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ContentJpaEntity() {
        // JPA가 리플렉션으로 객체를 만들 때 사용합니다.
    }

    private ContentJpaEntity(Episode episode) {
        this.id = episode.id();
        this.title = episode.title();
        this.summary = episode.summary();
        this.category = episode.category();
        this.script = episode.script();
        this.createdAt = episode.createdAt();
        this.updatedAt = episode.updatedAt();
    }

    static ContentJpaEntity from(Episode episode) {
        return new ContentJpaEntity(episode);
    }

    Episode toDomain() {
        return new Episode(id, title, summary, category, script, createdAt, updatedAt);
    }
}
