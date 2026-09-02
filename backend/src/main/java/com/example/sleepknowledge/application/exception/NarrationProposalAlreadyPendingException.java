package com.example.sleepknowledge.application.exception;

import java.util.UUID;

public class NarrationProposalAlreadyPendingException extends RuntimeException {

    public NarrationProposalAlreadyPendingException(UUID contentId) {
        super("이미 검토 중인 음성 지원 제안이 있습니다: " + contentId);
    }
}
