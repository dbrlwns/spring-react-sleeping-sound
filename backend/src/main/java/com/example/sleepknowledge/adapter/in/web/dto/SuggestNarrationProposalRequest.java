package com.example.sleepknowledge.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** 사용자가 제안하는 희망 음성입니다. 최종 음성은 관리자가 승인할 때 변경할 수 있습니다. */
public record SuggestNarrationProposalRequest(
        @NotBlank(message = "희망 음성을 선택해 주세요.")
        String preferredVoiceId
) {
}
