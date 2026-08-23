package com.example.sleepknowledge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NarrationPropertiesTest {

    @Test
    void 기본값은_ADC가_필요없는_macos_say이고_Chirp3_Kore_설정을_준비한다() {
        var properties = new NarrationProperties(null, null, null);

        assertThat(properties.provider()).isEqualTo(NarrationProperties.Provider.MACOS_SAY);
        assertThat(properties.macosSay().command()).isEqualTo("/usr/bin/say");
        assertThat(properties.googleChirp3().languageCode()).isEqualTo("ko-KR");
        assertThat(properties.googleChirp3().voiceName()).isEqualTo("ko-KR-Chirp3-HD-Kore");
        assertThat(properties.googleChirp3().maxInputBytes()).isEqualTo(5_000);
        assertThat(properties.googleChirp3().rpcTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void Spring_relaxed_binding으로_환경변수와_같은_설정명을_바인딩한다() {
        var source = new MapConfigurationPropertySource(Map.of(
                "app.narration.provider", "google-chirp3",
                "app.narration.google-chirp3.voice-name", "ko-KR-Chirp3-HD-Aoede",
                "app.narration.google-chirp3.max-input-bytes", "4000"
        ));

        NarrationProperties properties = new Binder(source)
                .bind("app.narration", Bindable.of(NarrationProperties.class))
                .get();

        assertThat(properties.provider()).isEqualTo(NarrationProperties.Provider.GOOGLE_CHIRP3);
        assertThat(properties.googleChirp3().voiceName()).isEqualTo("ko-KR-Chirp3-HD-Aoede");
        assertThat(properties.googleChirp3().maxInputBytes()).isEqualTo(4_000);
    }

    @Test
    void Cloud_한도와_로컬_메모리_방어값을_검증한다() {
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, null, null, 5_001, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 5000");
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, null, null, null, null, -1, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-audio-bytes");
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, null, null, null, null, null, Duration.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rpc-timeout");
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, "ko-KR", "en-US-Chirp3-HD-Kore", null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("language-code");
    }
}
