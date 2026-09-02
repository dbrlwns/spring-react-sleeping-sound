package com.example.sleepknowledge.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EpisodeDraftTest {

    @Test
    void 원고는_공백을_포함해_5000자까지_허용한다() {
        String script = "가" + " ".repeat(4_998) + "나";

        EpisodeDraft draft = new EpisodeDraft(
                "제목",
                "요약",
                ContentCategory.SCIENCE,
                script
        );

        assertThat(script).hasSize(EpisodeDraft.MAX_SCRIPT_LENGTH);
        assertThat(draft.script()).isEqualTo(script);
    }

    @Test
    void 원고는_앞뒤_공백도_포함해_5001자부터_거절한다() {
        String script = "가" + " ".repeat(5_000);

        assertThat(script).hasSize(EpisodeDraft.MAX_SCRIPT_LENGTH + 1);
        assertThatThrownBy(() -> new EpisodeDraft(
                "제목",
                "요약",
                ContentCategory.SCIENCE,
                script
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("script must not exceed 5000 characters");
    }
}
