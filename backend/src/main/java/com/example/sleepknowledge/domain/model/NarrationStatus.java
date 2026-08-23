package com.example.sleepknowledge.domain.model;

/** 저장된 콘텐츠에서 파생되는 자동 내레이션의 현재 처리 상태입니다. */
public enum NarrationStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
