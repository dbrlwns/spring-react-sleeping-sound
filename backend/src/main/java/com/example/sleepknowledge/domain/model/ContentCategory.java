package com.example.sleepknowledge.domain.model;

/** 화면과 API가 공유하는 안정적인 분류 코드입니다. 표시 이름은 각 클라이언트에서 번역할 수 있습니다. */
public enum ContentCategory {
    SCIENCE,
    SOCIETY,
    HISTORY,
    PHILOSOPHY,
    ECONOMY,
    TECHNOLOGY,
    CULTURE,
    PSYCHOLOGY,

    /** 기존 DB와 이전 클라이언트 호환을 위해 유지하는 과학 세부 분류입니다. */
    QUANTUM_PHYSICS,
    COSMOLOGY,
    ASTRONOMY,
    GENERAL_SCIENCE;

    /** API에서 노출할 범용 분류로 정규화합니다. */
    public ContentCategory canonical() {
        return switch (this) {
            case QUANTUM_PHYSICS, COSMOLOGY, ASTRONOMY, GENERAL_SCIENCE -> SCIENCE;
            default -> this;
        };
    }
}
