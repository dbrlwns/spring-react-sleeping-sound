package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.port.in.BrowseContentUseCase;
import com.example.sleepknowledge.application.port.in.CreateContentUseCase;
import com.example.sleepknowledge.application.port.in.UpdateContentUseCase;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;
import com.example.sleepknowledge.domain.model.NarrationState;
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
    private final Clock clock;

    public ContentService(
            ContentRepositoryPort contentRepository,
            NarrationAssetRepositoryPort narrationRepository,
            Clock clock
    ) {
        this.contentRepository = contentRepository;
        this.narrationRepository = narrationRepository;
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
    public Episode createContent(EpisodeDraft draft, String authorUsername) {
        Instant now = clock.instant();
        return contentRepository.save(Episode.create(UUID.randomUUID(), draft, authorUsername, now));
    }

    @Override
    @Transactional
    public Episode updateContent(UUID contentId, EpisodeDraft draft) {
        Episode existing = contentRepository.findByIdForUpdate(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        // 수정 요청을 보낸 계정과 무관하게 최초 작성자는 Episode.update가 그대로 보존합니다.
        Episode updated = contentRepository.save(existing.update(draft, clock.instant()));
        if (!existing.script().equals(updated.script())) {
            // 승인 당시 원고와 다른 MP3가 노출되지 않도록 즉시 제거하고 다시 제안받습니다.
            narrationRepository.deleteByContentId(contentId);
        }
        return updated;
    }
}
