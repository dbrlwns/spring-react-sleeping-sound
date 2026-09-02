package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.in.web.dto.AdminNarrationProposalResponse;
import com.example.sleepknowledge.adapter.in.web.dto.AdminNarrationProposalsResponse;
import com.example.sleepknowledge.adapter.in.web.dto.AdminNarrationVoicesResponse;
import com.example.sleepknowledge.adapter.in.web.dto.ApproveNarrationProposalRequest;
import com.example.sleepknowledge.application.port.in.BrowseContentUseCase;
import com.example.sleepknowledge.application.port.in.ReviewNarrationProposalUseCase;
import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminNarrationProposalController {

    private final ReviewNarrationProposalUseCase reviewUseCase;
    private final BrowseContentUseCase browseContentUseCase;

    public AdminNarrationProposalController(
            ReviewNarrationProposalUseCase reviewUseCase,
            BrowseContentUseCase browseContentUseCase
    ) {
        this.reviewUseCase = reviewUseCase;
        this.browseContentUseCase = browseContentUseCase;
    }

    @GetMapping("/narration-proposals")
    public AdminNarrationProposalsResponse list(
            @RequestParam(defaultValue = "PENDING") NarrationProposalStatus status
    ) {
        return new AdminNarrationProposalsResponse(
                reviewUseCase.list(status).stream().map(this::response).toList()
        );
    }

    @GetMapping("/narration/voices")
    public AdminNarrationVoicesResponse voices() {
        return AdminNarrationVoicesResponse.from(reviewUseCase.listVoiceOptions());
    }

    @PostMapping("/narration-proposals/{proposalId}/approve")
    public AdminNarrationProposalResponse approve(
            @PathVariable UUID proposalId,
            @Valid @RequestBody ApproveNarrationProposalRequest request,
            Authentication authentication
    ) {
        return response(reviewUseCase.approve(
                proposalId,
                request.voiceId(),
                authentication.getName()
        ));
    }

    @PostMapping("/narration-proposals/{proposalId}/reject")
    public AdminNarrationProposalResponse reject(
            @PathVariable UUID proposalId,
            Authentication authentication
    ) {
        return response(reviewUseCase.reject(proposalId, authentication.getName()));
    }

    private AdminNarrationProposalResponse response(NarrationProposal proposal) {
        String title = browseContentUseCase.getContent(proposal.contentId()).title();
        return AdminNarrationProposalResponse.from(proposal, title);
    }
}
