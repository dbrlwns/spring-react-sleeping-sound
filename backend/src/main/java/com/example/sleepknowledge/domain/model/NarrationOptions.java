package com.example.sleepknowledge.domain.model;

/** 콘텐츠 원문은 서버에서 가져오고, 클라이언트는 내레이션 옵션만 지정합니다. */
public record NarrationOptions(String voiceId, double speed) {

    public NarrationOptions {
        if (voiceId == null || voiceId.isBlank()) {
            throw new IllegalArgumentException("voiceId must not be blank");
        }
        if (!Double.isFinite(speed) || speed < 0.5 || speed > 2.0) {
            throw new IllegalArgumentException("speed must be between 0.5 and 2.0");
        }
        voiceId = voiceId.trim();
    }
}
