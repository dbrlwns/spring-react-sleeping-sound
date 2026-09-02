package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.example.sleepknowledge.domain.model.Voice;

import java.util.List;

public record AdminNarrationVoicesResponse(List<AdminNarrationVoiceResponse> voices) {

    public static AdminNarrationVoicesResponse from(List<Voice> voices) {
        return new AdminNarrationVoicesResponse(
                voices.stream()
                        .map(Voice::id)
                        .map(NarrationVoiceOption::fromVoiceId)
                        .map(AdminNarrationVoiceResponse::from)
                        .toList()
        );
    }
}
