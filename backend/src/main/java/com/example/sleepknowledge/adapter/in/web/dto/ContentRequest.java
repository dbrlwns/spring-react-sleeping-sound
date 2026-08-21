package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.EpisodeDraft;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** JSON 표현과 검증 규칙을 HTTP 경계에 두고 도메인 입력으로 변환합니다. */
public record ContentRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = EpisodeDraft.MAX_TITLE_LENGTH, message = "제목은 120자 이하여야 합니다.")
        String title,

        @NotBlank(message = "요약을 입력해 주세요.")
        @Size(max = EpisodeDraft.MAX_SUMMARY_LENGTH, message = "요약은 500자 이하여야 합니다.")
        String summary,

        @NotNull(message = "카테고리를 선택해 주세요.")
        ContentCategory category,

        @NotBlank(message = "원고를 입력해 주세요.")
        @Size(max = EpisodeDraft.MAX_SCRIPT_LENGTH, message = "원고는 20000자 이하여야 합니다.")
        String script
) {

    public EpisodeDraft toDraft() {
        return new EpisodeDraft(title, summary, category, script);
    }
}
