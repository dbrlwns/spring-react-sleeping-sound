package com.example.sleepknowledge.application.exception;

/** 외부 TTS 엔진 실행 실패를 애플리케이션 언어로 변환한 예외입니다. */
public class SpeechSynthesisException extends RuntimeException {

    public SpeechSynthesisException(String message) {
        super(message);
    }

    public SpeechSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}
