package com.example.sleepknowledge.application.exception;

public class NarrationProposalConflictException extends RuntimeException {

    public NarrationProposalConflictException() {
        super("이미 처리된 음성 지원 제안입니다.");
    }
}
