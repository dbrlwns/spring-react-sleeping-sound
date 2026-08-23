package com.example.sleepknowledge.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data가 런타임에 구현해 주는 내부 저장소입니다. 애플리케이션 계층에는 노출하지 않습니다. */
interface SpringDataContentRepository extends JpaRepository<ContentJpaEntity, UUID> {

    List<ContentJpaEntity> findAllByOrderByUpdatedAtDesc();
}
