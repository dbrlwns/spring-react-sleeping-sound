package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.Episode;

import java.util.List;

public record ContentsResponse(List<ContentSummaryResponse> contents) {

    public static ContentsResponse from(List<Episode> episodes) {
        return new ContentsResponse(episodes.stream().map(ContentSummaryResponse::from).toList());
    }
}
