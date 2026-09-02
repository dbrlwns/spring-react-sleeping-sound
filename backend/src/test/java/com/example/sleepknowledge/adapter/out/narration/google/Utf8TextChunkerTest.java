package com.example.sleepknowledge.adapter.out.narration.google;

import com.example.sleepknowledge.domain.model.EpisodeDraft;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Utf8TextChunkerTest {

    @Test
    void 한글과_emoji를_UTF8_한도_안에서_원문_그대로_나눈다() {
        String text = "별빛 아래에서 과학을 읽어요 😊 다음 문장도 그대로 이어집니다.";

        var chunks = Utf8TextChunker.split(text, 19);

        assertThat(String.join("", chunks)).isEqualTo(text);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(19)
        );
        assertThat(chunks).noneMatch(String::isEmpty);
    }

    @Test
    void 가능한_경우_문장_경계를_공백보다_우선한다() {
        String text = "첫 문장입니다. 다음 문장을 읽습니다.";

        var chunks = Utf8TextChunker.split(text, 30);

        assertThat(chunks.get(0)).isEqualTo("첫 문장입니다. ");
        assertThat(String.join("", chunks)).isEqualTo(text);
    }

    @Test
    void surrogate_pair인_emoji_중간을_자르지_않는다() {
        var chunks = Utf8TextChunker.split("가😊나", 4);

        assertThat(chunks).containsExactly("가", "😊", "나");
    }

    @Test
    void code_point_하나가_한도보다_크면_명확히_거절한다() {
        assertThatThrownBy(() -> Utf8TextChunker.split("😊", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code point");
    }

    @Test
    void 현재_최대_원고_5000자도_Cloud_한도_안에서_분할한다() {
        String text = "가".repeat(EpisodeDraft.MAX_SCRIPT_LENGTH);

        var chunks = Utf8TextChunker.split(text, 5_000);

        assertThat(String.join("", chunks)).isEqualTo(text);
        assertThat(chunks.size()).isLessThanOrEqualTo(32);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getBytes(StandardCharsets.UTF_8).length)
                        .isLessThanOrEqualTo(5_000));
    }
}
