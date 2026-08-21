package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.port.in.BrowseContentUseCase;
import com.example.sleepknowledge.application.port.in.CreateContentUseCase;
import com.example.sleepknowledge.application.port.in.UpdateContentUseCase;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 콘텐츠 관련 비즈니스 흐름을 담당하며 HTTP와 JPA를 알지 못합니다. */
@Service
@Transactional(readOnly = true)
public class ContentService implements BrowseContentUseCase, CreateContentUseCase, UpdateContentUseCase {

    private final ContentRepositoryPort contentRepository;
    private final Clock clock;

    public ContentService(ContentRepositoryPort contentRepository, Clock clock) {
        this.contentRepository = contentRepository;
        this.clock = clock;
    }

    @Override
    public List<Episode> listContents() {
        return contentRepository.findAll();
    }

    @Override
    public Episode getContent(UUID contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    @Override
    @Transactional
    public Episode createContent(EpisodeDraft draft) {
        Instant now = clock.instant();
        return contentRepository.save(Episode.create(UUID.randomUUID(), draft, now));
    }

    @Override
    @Transactional
    public Episode updateContent(UUID contentId, EpisodeDraft draft) {
        Episode existing = getContent(contentId);
        return contentRepository.save(existing.update(draft, clock.instant()));
    }
}
