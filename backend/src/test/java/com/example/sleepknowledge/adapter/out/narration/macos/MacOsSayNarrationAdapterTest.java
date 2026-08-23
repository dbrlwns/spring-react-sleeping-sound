package com.example.sleepknowledge.adapter.out.narration.macos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MacOsSayNarrationAdapterTest {

    @Test
    void say_출력에서_이름_언어_설명을_분리한다() {
        String output = """
                Samantha             en_US    # Hello! My name is Samantha.
                Majed                ar_001   # مرحبًا! اسمي ماجد.
                Yuna                 ko_KR    # 안녕하세요! 제 이름은 유나입니다.
                """;

        var voices = MacOsSayNarrationAdapter.parseVoices(output);

        assertThat(voices).hasSize(3);
        assertThat(voices)
                .extracting("id", "locale")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Majed", "ar_001"),
                        org.assertj.core.groups.Tuple.tuple("Samantha", "en_US"),
                        org.assertj.core.groups.Tuple.tuple("Yuna", "ko_KR")
                );
    }
}
