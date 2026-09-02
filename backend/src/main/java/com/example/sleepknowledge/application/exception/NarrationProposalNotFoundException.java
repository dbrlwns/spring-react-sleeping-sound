package com.example.sleepknowledge.application.exception;

import java.util.UUID;

public class NarrationProposalNotFoundException extends RuntimeException {

    public NarrationProposalNotFoundException(UUID proposalId) {
        super("음성 지원 제안을 찾을 수 없습니다: " + proposalId);
    }
}
