package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.in.web.dto.NarrationProposalResponse;
import com.example.sleepknowledge.adapter.in.web.dto.SuggestNarrationProposalRequest;
import com.example.sleepknowledge.application.port.in.SuggestNarrationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contents/{contentId}/narration-proposals")
public class NarrationProposalController {

    private final SuggestNarrationUseCase useCase;

    public NarrationProposalController(SuggestNarrationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<NarrationProposalResponse> suggest(
            @PathVariable UUID contentId,
            @Valid @RequestBody SuggestNarrationProposalRequest request,
            Authentication authentication
    ) {
        NarrationProposalResponse created = NarrationProposalResponse.from(
                useCase.suggest(contentId, authentication.getName(), request.preferredVoiceId())
        );
        return ResponseEntity.created(URI.create(
                        "/api/v1/contents/" + contentId + "/narration-proposals/latest"
                ))
                .body(created);
    }

    @GetMapping("/latest")
    public ResponseEntity<NarrationProposalResponse> latest(@PathVariable UUID contentId) {
        return useCase.findLatest(contentId)
                .map(NarrationProposalResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
