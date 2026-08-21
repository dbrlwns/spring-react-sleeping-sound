package com.example.sleepknowledge.application.exception;

public class UnsupportedVoiceException extends RuntimeException {

    public UnsupportedVoiceException(String voiceId) {
        super("사용할 수 없는 음성입니다: " + voiceId);
    }
}
