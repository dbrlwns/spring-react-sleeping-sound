package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;

import java.time.Instant;

public record NarrationStatusResponse(
        NarrationStatus status,
        String errorMessage,
        Instant updatedAt,
        String audioUrl
) {

    public static NarrationStatusResponse from(NarrationState state) {
        String audioUrl = state.status() == NarrationStatus.READY
                ? "/api/v1/contents/" + state.contentId() + "/narration/audio"
                : null;
        return new NarrationStatusResponse(
                state.status(),
                state.errorMessage(),
                state.updatedAt(),
                audioUrl
        );
    }
}
