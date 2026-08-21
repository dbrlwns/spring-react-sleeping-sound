package com.example.sleepknowledge.application.exception;

import java.util.UUID;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException(UUID contentId) {
        super("콘텐츠를 찾을 수 없습니다: " + contentId);
    }
}
