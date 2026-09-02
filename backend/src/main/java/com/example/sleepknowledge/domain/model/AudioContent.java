package com.example.sleepknowledge.domain.model;

import java.util.Arrays;

/** 생성된 내레이션과 표현 형식을 함께 전달하는 불변 값 객체입니다. */
public final class AudioContent {

    public static final String LINEAR16_WAV_MEDIA_TYPE = "audio/wav";
    public static final String MP3_MEDIA_TYPE = "audio/mpeg";

    private final byte[] bytes;
    private final String mediaType;

    private AudioContent(byte[] bytes, String mediaType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("audio bytes must not be empty");
        }
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.mediaType = mediaType;
    }

    /** Google TTS 청크를 합친, MP3 인코딩 전 내부 중간 산출물입니다. */
    public static AudioContent linear16Wav(byte[] bytes) {
        return new AudioContent(bytes, LINEAR16_WAV_MEDIA_TYPE);
    }

    /** HTTP 응답과 영속 저장에 사용하는 최종 MP3입니다. */
    public static AudioContent mp3(byte[] bytes) {
        return new AudioContent(bytes, MP3_MEDIA_TYPE);
    }

    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public String mediaType() {
        return mediaType;
    }
}
