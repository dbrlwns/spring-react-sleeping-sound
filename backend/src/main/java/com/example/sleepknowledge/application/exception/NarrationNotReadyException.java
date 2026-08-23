package com.example.sleepknowledge.application.exception;

import com.example.sleepknowledge.domain.model.NarrationStatus;

import java.util.Objects;
import java.util.UUID;

public class NarrationNotReadyException extends RuntimeException {

    private final NarrationStatus status;

    public NarrationNotReadyException(UUID contentId, NarrationStatus status) {
        super("콘텐츠 " + contentId + "의 내레이션이 아직 준비되지 않았습니다. (status=" + status + ")");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public NarrationStatus status() {
        return status;
    }
}
