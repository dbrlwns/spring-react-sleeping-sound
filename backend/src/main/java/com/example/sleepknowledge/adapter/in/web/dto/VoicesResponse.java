package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.Voice;

import java.util.List;

public record VoicesResponse(List<VoiceResponse> voices) {

    public static VoicesResponse from(List<Voice> voices) {
        return new VoicesResponse(voices.stream().map(VoiceResponse::from).toList());
    }
}
