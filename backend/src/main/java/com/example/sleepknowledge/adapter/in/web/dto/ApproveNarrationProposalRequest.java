package com.example.sleepknowledge.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveNarrationProposalRequest(
        @NotBlank(message = "음성을 선택해 주세요.")
        String voiceId
) {
}
