package com.example.sleepknowledge.adapter.out.narration.google;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Linear16WavTest {

    @Test
    void 확장_fmt와_홀수_padding_추가_chunk를_건너뛰고_PCM만_합친다() {
        byte[] first = WavTestFixtures.wav(24_000, new byte[]{1, 2, 3, 4}, false, true);
        byte[] second = WavTestFixtures.wav(24_000, new byte[]{5, 6, 7, 8}, true, true);

        byte[] merged = Linear16Wav.merge(List.of(first, second), 100);

        assertThat(new String(merged, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(merged, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WAVE");
        assertThat(readInt(merged, 4)).isEqualTo(merged.length - 8);
        assertThat(readInt(merged, 40)).isEqualTo(8);
        assertThat(Arrays.copyOfRange(merged, merged.length - 8, merged.length))
                .containsExactly((byte) 1, (byte) 2, (byte) 3, (byte) 4,
                        (byte) 5, (byte) 6, (byte) 7, (byte) 8);
    }

    @Test
    void 단일_chunk도_RIFF를_검증하고_정규화한다() {
        byte[] wav = WavTestFixtures.wav(22_050, new byte[]{1, 2, 3, 4}, true, true);

        byte[] normalized = Linear16Wav.merge(List.of(wav), 100);

        assertThat(normalized).hasSize(48);
        assertThat(readInt(normalized, 24)).isEqualTo(22_050);
        assertThat(Arrays.copyOfRange(normalized, normalized.length - 4, normalized.length))
                .containsExactly((byte) 1, (byte) 2, (byte) 3, (byte) 4);
    }

    @Test
    void sample_rate가_다른_WAV는_합치지_않는다() {
        byte[] first = WavTestFixtures.wav(24_000, new byte[]{1, 2}, false, false);
        byte[] second = WavTestFixtures.wav(22_050, new byte[]{3, 4}, false, false);

        assertThatThrownBy(() -> Linear16Wav.merge(List.of(first, second), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formats do not match");
    }

    @Test
    void 손상된_RIFF_크기와_PCM_frame을_거절한다() {
        byte[] truncated = WavTestFixtures.wav(24_000, new byte[]{1, 2}, false, false);
        truncated[4] = 0;
        byte[] oddPcm = WavTestFixtures.wav(24_000, new byte[]{1}, false, false);

        assertThatThrownBy(() -> Linear16Wav.merge(List.of(truncated), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RIFF size");
        assertThatThrownBy(() -> Linear16Wav.merge(List.of(oddPcm), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aligned");
    }

    @Test
    void 설정된_최대_audio_data_크기를_넘지_않는다() {
        byte[] wav = WavTestFixtures.wav(24_000, new byte[]{1, 2, 3, 4}, false, false);

        assertThatThrownBy(() -> Linear16Wav.merge(List.of(wav), 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxAudioBytes");
    }

    private static int readInt(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }
}
