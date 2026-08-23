package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;

import java.time.Instant;
import java.util.UUID;

/** 목록에서는 긴 원고를 제외해 응답을 가볍게 유지합니다. */
public record ContentSummaryResponse(
        UUID id,
        String title,
        String summary,
        ContentCategory category,
        Instant createdAt,
        Instant updatedAt
) {

    public static ContentSummaryResponse from(Episode episode) {
        return new ContentSummaryResponse(
                episode.id(),
                episode.title(),
                episode.summary(),
                episode.category().canonical(),
                episode.createdAt(),
                episode.updatedAt()
        );
    }
}
