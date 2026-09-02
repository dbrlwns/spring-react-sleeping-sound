package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.in.web.dto.ContentDetailResponse;
import com.example.sleepknowledge.adapter.in.web.dto.ContentRequest;
import com.example.sleepknowledge.adapter.in.web.dto.ContentsResponse;
import com.example.sleepknowledge.application.port.in.BrowseContentUseCase;
import com.example.sleepknowledge.application.port.in.CreateContentUseCase;
import com.example.sleepknowledge.application.port.in.UpdateContentUseCase;
import com.example.sleepknowledge.domain.model.Episode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** HTTP 요청을 콘텐츠 유스케이스로 번역하는 inbound adapter입니다. */
@RestController
@RequestMapping("/api/v1/contents")
public class ContentController {

    private final BrowseContentUseCase browseContentUseCase;
    private final CreateContentUseCase createContentUseCase;
    private final UpdateContentUseCase updateContentUseCase;

    public ContentController(
            BrowseContentUseCase browseContentUseCase,
            CreateContentUseCase createContentUseCase,
            UpdateContentUseCase updateContentUseCase
    ) {
        this.browseContentUseCase = browseContentUseCase;
        this.createContentUseCase = createContentUseCase;
        this.updateContentUseCase = updateContentUseCase;
    }

    @GetMapping
    public ContentsResponse listContents() {
        return ContentsResponse.from(browseContentUseCase.listContents());
    }

    @GetMapping("/{contentId}")
    public ContentDetailResponse getContent(@PathVariable UUID contentId) {
        return ContentDetailResponse.from(browseContentUseCase.getContent(contentId));
    }

    @PostMapping
    public ResponseEntity<ContentDetailResponse> createContent(
            @Valid @RequestBody ContentRequest request,
            Authentication authentication
    ) {
        // 작성자는 클라이언트 JSON이 아니라 Spring Security가 검증한 principal에서만 가져옵니다.
        Episode created = createContentUseCase.createContent(request.toDraft(), authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/contents/" + created.id()))
                .body(ContentDetailResponse.from(created));
    }

    @PutMapping("/{contentId}")
    public ContentDetailResponse updateContent(
            @PathVariable UUID contentId,
            @Valid @RequestBody ContentRequest request
    ) {
        return ContentDetailResponse.from(updateContentUseCase.updateContent(contentId, request.toDraft()));
    }
}
