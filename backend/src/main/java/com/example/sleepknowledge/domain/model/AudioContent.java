package com.example.sleepknowledge.domain.model;

import java.util.Arrays;

/** 생성된 내레이션과 표현 형식을 함께 전달하는 불변 값 객체입니다. */
public final class AudioContent {

    public static final String WAV_MEDIA_TYPE = "audio/wav";

    private final byte[] bytes;
    private final String mediaType;

    private AudioContent(byte[] bytes, String mediaType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("audio bytes must not be empty");
        }
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.mediaType = mediaType;
    }

    public static AudioContent wav(byte[] bytes) {
        return new AudioContent(bytes, WAV_MEDIA_TYPE);
    }

    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public String mediaType() {
        return mediaType;
    }
}
