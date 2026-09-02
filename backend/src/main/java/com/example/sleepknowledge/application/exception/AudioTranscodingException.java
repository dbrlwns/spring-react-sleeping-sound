package com.example.sleepknowledge.application.exception;

/** 내부 WAV를 배포 형식으로 인코딩하지 못했을 때 사용하는 애플리케이션 예외입니다. */
public class AudioTranscodingException extends RuntimeException {

    public AudioTranscodingException(String message) {
        super(message);
    }

    public AudioTranscodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
