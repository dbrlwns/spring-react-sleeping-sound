package com.example.sleepknowledge.domain.model;

/** 관리자 승인 후 생성되는 내레이션의 현재 처리 상태입니다. */
public enum NarrationStatus {
    NOT_REQUESTED,
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
