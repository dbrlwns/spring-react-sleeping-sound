package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;

import java.time.Instant;
import java.util.UUID;

public record ContentDetailResponse(
        UUID id,
        String title,
        String summary,
        ContentCategory category,
        String script,
        Instant createdAt,
        Instant updatedAt
) {

    public static ContentDetailResponse from(Episode episode) {
        return new ContentDetailResponse(
                episode.id(),
                episode.title(),
                episode.summary(),
                episode.category(),
                episode.script(),
                episode.createdAt(),
                episode.updatedAt()
        );
    }
}
