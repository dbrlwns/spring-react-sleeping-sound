package com.example.sleepknowledge.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * 관리자가 선택할 수 있는 Chirp 3 HD 음성을 서버의 허용 목록으로 고정합니다.
 * 클라이언트가 임의의 Cloud voice ID를 보내도 이 목록 밖이면 합성하지 않습니다.
 */
public enum NarrationVoiceOption {
    KORE("ko-KR-Chirp3-HD-Kore", "Kore", "여성 · 안정적이고 단정한 음성", Gender.FEMALE),
    ZEPHYR("ko-KR-Chirp3-HD-Zephyr", "Zephyr", "여성 · 밝은 음성", Gender.FEMALE),
    LEDA("ko-KR-Chirp3-HD-Leda", "Leda", "여성 · 젊은 느낌의 음성", Gender.FEMALE),
    CHARON("ko-KR-Chirp3-HD-Charon", "Charon", "남성 · 정보 전달에 어울리는 음성", Gender.MALE),
    SCHEDAR("ko-KR-Chirp3-HD-Schedar", "Schedar", "남성 · 균형 잡힌 음성", Gender.MALE),
    ACHIRD("ko-KR-Chirp3-HD-Achird", "Achird", "남성 · 친근한 음성", Gender.MALE);

    private final String voiceId;
    private final String displayName;
    private final String description;
    private final Gender gender;

    NarrationVoiceOption(String voiceId, String displayName, String description, Gender gender) {
        this.voiceId = voiceId;
        this.displayName = displayName;
        this.description = description;
        this.gender = gender;
    }

    public String voiceId() {
        return voiceId;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Gender gender() {
        return gender;
    }

    public Voice toVoice() {
        return new Voice(voiceId, displayName, description, "ko-KR");
    }

    public static NarrationVoiceOption fromVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            throw new IllegalArgumentException("voiceId는 필수입니다.");
        }
        return Arrays.stream(values())
                .filter(option -> option.voiceId.equals(voiceId.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("선택할 수 없는 Chirp 3 음성입니다."));
    }

    public static List<Voice> voices() {
        return Arrays.stream(values()).map(NarrationVoiceOption::toVoice).toList();
    }

    public enum Gender {
        FEMALE,
        MALE
    }
}
