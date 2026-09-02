package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.NarrationVoiceOption;

public record AdminNarrationVoiceResponse(
        String id,
        String name,
        String description,
        String locale,
        NarrationVoiceOption.Gender gender
) {

    public static AdminNarrationVoiceResponse from(NarrationVoiceOption option) {
        return new AdminNarrationVoiceResponse(
                option.voiceId(),
                option.displayName(),
                option.description(),
                "ko-KR",
                option.gender()
        );
    }
}
