package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.domain.model.Episode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA를 애플리케이션의 저장소 port로 번역하는 outbound adapter입니다. */
@Repository
public class JpaContentRepositoryAdapter implements ContentRepositoryPort {

    private final SpringDataContentRepository repository;

    public JpaContentRepositoryAdapter(SpringDataContentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Episode> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(ContentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Episode> findById(UUID contentId) {
        return repository.findById(contentId).map(ContentJpaEntity::toDomain);
    }

    @Override
    public Optional<Episode> findByIdForUpdate(UUID contentId) {
        return repository.findByIdForUpdate(contentId).map(ContentJpaEntity::toDomain);
    }

    @Override
    public Episode save(Episode episode) {
        return repository.save(ContentJpaEntity.from(episode)).toDomain();
    }

    @Override
    public long count() {
        return repository.count();
    }
}
