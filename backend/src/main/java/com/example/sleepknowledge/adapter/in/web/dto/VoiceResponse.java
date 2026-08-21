package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.Voice;

public record VoiceResponse(String id, String name, String description, String locale) {

    public static VoiceResponse from(Voice voice) {
        return new VoiceResponse(voice.id(), voice.name(), voice.description(), voice.locale());
    }
}
