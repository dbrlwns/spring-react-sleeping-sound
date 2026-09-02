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
    void 기본값은_Chirp3_요청한도를_준비한다() {
        var properties = new NarrationProperties(null, null);

        assertThat(properties.googleChirp3().maxInputBytes()).isEqualTo(5_000);
        assertThat(properties.googleChirp3().maxAudioBytes()).isEqualTo(128 * 1024 * 1024);
        assertThat(properties.googleChirp3().rpcTimeout()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.ffmpeg().command()).isEqualTo("ffmpeg");
        assertThat(properties.ffmpeg().timeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.ffmpeg().maxOutputBytes()).isEqualTo(64 * 1024 * 1024);
        assertThat(properties.ffmpeg().bitrateKbps()).isEqualTo(64);
    }

    @Test
    void FFmpeg_설정을_relaxed_binding으로_바인딩한다() {
        var source = new MapConfigurationPropertySource(Map.of(
                "app.narration.ffmpeg.command", "/opt/homebrew/bin/ffmpeg",
                "app.narration.ffmpeg.timeout", "2m",
                "app.narration.ffmpeg.max-output-bytes", "1048576",
                "app.narration.ffmpeg.bitrate-kbps", "48"
        ));

        NarrationProperties properties = new Binder(source)
                .bind("app.narration", Bindable.of(NarrationProperties.class))
                .get();

        assertThat(properties.ffmpeg().command()).isEqualTo("/opt/homebrew/bin/ffmpeg");
        assertThat(properties.ffmpeg().timeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.ffmpeg().maxOutputBytes()).isEqualTo(1_048_576);
        assertThat(properties.ffmpeg().bitrateKbps()).isEqualTo(48);
    }

    @Test
    void FFmpeg_시간_크기_bitrate_방어값을_검증한다() {
        assertThatThrownBy(() -> new NarrationProperties.Ffmpeg(null, Duration.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ffmpeg.timeout");
        assertThatThrownBy(() -> new NarrationProperties.Ffmpeg(
                null, Duration.ofHours(2), null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 hour");
        assertThatThrownBy(() -> new NarrationProperties.Ffmpeg(null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-output-bytes");
        assertThatThrownBy(() -> new NarrationProperties.Ffmpeg(null, null, null, 321))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bitrate-kbps");
    }

    @Test
    void Spring_relaxed_binding으로_환경변수와_같은_설정명을_바인딩한다() {
        var source = new MapConfigurationPropertySource(Map.of(
                "app.narration.google-chirp3.max-input-bytes", "4000"
        ));

        NarrationProperties properties = new Binder(source)
                .bind("app.narration", Bindable.of(NarrationProperties.class))
                .get();

        assertThat(properties.googleChirp3().maxInputBytes()).isEqualTo(4_000);
    }

    @Test
    void Cloud_한도와_로컬_메모리_방어값을_검증한다() {
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, 5_001, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 4 and 5000");
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, null, null, -1, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-audio-bytes");
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, null, null, null, Duration.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rpc-timeout");
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, 3, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 4 and 5000");
        assertThat(new NarrationProperties.GoogleChirp3(
                null, 4, null, null, null
        ).maxInputBytes()).isEqualTo(4);
        assertThatThrownBy(() -> new NarrationProperties.GoogleChirp3(
                null, null, 65, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 64");
    }
}
