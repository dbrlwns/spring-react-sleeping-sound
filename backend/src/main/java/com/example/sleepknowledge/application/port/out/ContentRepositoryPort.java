package com.example.sleepknowledge.application.port.out;

import com.example.sleepknowledge.domain.model.Episode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 애플리케이션이 저장소에 요구하는 계약이며 JPA 세부사항을 노출하지 않습니다. */
public interface ContentRepositoryPort {

    List<Episode> findAll();

    Optional<Episode> findById(UUID contentId);

    /** 상태 변경 흐름에서 콘텐츠와 파생 asset의 순서를 직렬화합니다. */
    Optional<Episode> findByIdForUpdate(UUID contentId);

    Episode save(Episode episode);

    long count();
}
