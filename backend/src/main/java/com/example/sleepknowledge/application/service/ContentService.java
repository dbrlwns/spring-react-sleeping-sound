package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.port.in.BrowseContentUseCase;
import com.example.sleepknowledge.application.port.in.CreateContentUseCase;
import com.example.sleepknowledge.application.port.in.UpdateContentUseCase;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;
import com.example.sleepknowledge.domain.model.NarrationState;
import org.springframework.context.ApplicationEventPublisher;
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
    private final NarrationAssetRepositoryPort narrationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ContentService(
            ContentRepositoryPort contentRepository,
            NarrationAssetRepositoryPort narrationRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.contentRepository = contentRepository;
        this.narrationRepository = narrationRepository;
        this.eventPublisher = eventPublisher;
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
        Episode created = contentRepository.save(Episode.create(UUID.randomUUID(), draft, now));
        resetAndRequestNarration(created);
        return created;
    }

    @Override
    @Transactional
    public Episode updateContent(UUID contentId, EpisodeDraft draft) {
        Episode existing = getContent(contentId);
        Episode updated = contentRepository.save(existing.update(draft, clock.instant()));
        resetAndRequestNarration(updated);
        return updated;
    }

    private void resetAndRequestNarration(Episode episode) {
        NarrationState state = narrationRepository.resetToPending(
                episode.id(),
                UUID.randomUUID(),
                episode.updatedAt(),
                clock.instant()
        );
        eventPublisher.publishEvent(new NarrationGenerationRequested(
                state.contentId(),
                state.generationId(),
                state.sourceUpdatedAt()
        ));
    }
}
