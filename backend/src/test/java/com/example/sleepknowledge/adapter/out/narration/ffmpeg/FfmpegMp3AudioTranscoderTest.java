package com.example.sleepknowledge.adapter.out.narration.ffmpeg;

import com.example.sleepknowledge.application.exception.AudioTranscodingException;
import com.example.sleepknowledge.config.NarrationProperties;
import com.example.sleepknowledge.domain.model.AudioContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledOnOs({OS.LINUX, OS.MAC})
class FfmpegMp3AudioTranscoderTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void ProcessBuilder_인자를_통해_MP3를_만들고_audio_mpeg로_반환한다() throws IOException {
        Path executable = executable("fake-ffmpeg-success", """
                #!/bin/sh
                for argument in "$@"; do
                  output="$argument"
                done
                if [ "$output" = "pipe:1" ]; then
                  cat > /dev/null
                  exit 0
                fi
                printf '\\377\\373\\220\\144' > "$output"
                """);
        var transcoder = new FfmpegMp3AudioTranscoder(properties(
                executable, Duration.ofSeconds(2), 1_024
        ));

        AudioContent result = transcoder.encodeMp3(
                AudioContent.linear16Wav(new byte[]{82, 73, 70, 70})
        );

        assertThat(result.mediaType()).isEqualTo(AudioContent.MP3_MEDIA_TYPE);
        assertThat(result.bytes()).containsExactly(
                (byte) 0xff, (byte) 0xfb, (byte) 0x90, (byte) 0x64
        );
    }

    @Test
    void 인코딩_시간과_출력_크기를_제한한다() throws IOException {
        Path slowExecutable = executable("fake-ffmpeg-slow", """
                #!/bin/sh
                for argument in "$@"; do
                  output="$argument"
                done
                if [ "$output" = "pipe:1" ]; then
                  cat > /dev/null
                  exit 0
                fi
                while :; do :; done
                """);
        var timeoutTranscoder = new FfmpegMp3AudioTranscoder(properties(
                slowExecutable, Duration.ofMillis(50), 1_024
        ));

        assertThatThrownBy(() -> timeoutTranscoder.encodeMp3(
                AudioContent.linear16Wav(new byte[]{82, 73, 70, 70})
        )).isInstanceOf(AudioTranscodingException.class)
                .hasMessage("MP3 인코딩 제한 시간을 초과했습니다.");

        Path largeOutputExecutable = executable("fake-ffmpeg-large", """
                #!/bin/sh
                for argument in "$@"; do
                  output="$argument"
                done
                if [ "$output" = "pipe:1" ]; then
                  cat > /dev/null
                  exit 0
                fi
                printf '\\377\\373\\220\\144\\377\\373\\220\\144' > "$output"
                """);
        var boundedTranscoder = new FfmpegMp3AudioTranscoder(properties(
                largeOutputExecutable, Duration.ofSeconds(2), 4
        ));

        assertThatThrownBy(() -> boundedTranscoder.encodeMp3(
                AudioContent.linear16Wav(new byte[]{82, 73, 70, 70})
        )).isInstanceOf(AudioTranscodingException.class)
                .hasMessage("인코딩된 MP3가 허용된 크기를 초과했습니다.");
    }

    @Test
    void FFmpeg가_없거나_입력이_WAV가_아니면_합성_전에_명확히_거절한다() {
        Path missing = temporaryDirectory.resolve("missing-ffmpeg");
        var transcoder = new FfmpegMp3AudioTranscoder(properties(
                missing, Duration.ofSeconds(1), 1_024
        ));

        assertThatThrownBy(transcoder::verifyAvailable)
                .isInstanceOf(AudioTranscodingException.class)
                .hasMessageContaining("FFmpeg를 실행하지 못했습니다");
        assertThatThrownBy(() -> transcoder.encodeMp3(AudioContent.mp3(new byte[]{1})))
                .isInstanceOf(AudioTranscodingException.class)
                .hasMessage("MP3 인코딩 입력은 LINEAR16 WAV여야 합니다.");
    }

    @Test
    void FFmpeg는_있지만_libmp3lame이_없으면_사전_점검에서_거절한다() throws IOException {
        Path executable = executable("fake-ffmpeg-without-lame", """
                #!/bin/sh
                cat > /dev/null
                exit 7
                """);
        var transcoder = new FfmpegMp3AudioTranscoder(properties(
                executable, Duration.ofSeconds(1), 1_024
        ));

        assertThatThrownBy(transcoder::verifyAvailable)
                .isInstanceOf(AudioTranscodingException.class)
                .hasMessage("FFmpeg의 libmp3lame MP3 인코더를 사용할 수 없습니다.");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_REAL_FFMPEG_TEST", matches = "true")
    void 설치된_실제_FFmpeg로_LINEAR16_WAV를_MP3로_변환한다() {
        var transcoder = new FfmpegMp3AudioTranscoder(new NarrationProperties.Ffmpeg(
                "ffmpeg", Duration.ofSeconds(10), 1_048_576, 64
        ));

        AudioContent result = transcoder.encodeMp3(AudioContent.linear16Wav(silentWav()));

        assertThat(result.mediaType()).isEqualTo(AudioContent.MP3_MEDIA_TYPE);
        assertThat(result.bytes()).hasSizeGreaterThan(100);
    }

    private NarrationProperties.Ffmpeg properties(
            Path executable,
            Duration timeout,
            int maxOutputBytes
    ) {
        return new NarrationProperties.Ffmpeg(
                executable.toString(), timeout, maxOutputBytes, 64
        );
    }

    private Path executable(String name, String script) throws IOException {
        Path path = temporaryDirectory.resolve(name);
        Files.writeString(path, script, StandardCharsets.UTF_8);
        if (!path.toFile().setExecutable(true)) {
            throw new IOException("test executable permission could not be set");
        }
        return path;
    }

    private static byte[] silentWav() {
        int sampleRate = 24_000;
        int pcmBytes = sampleRate / 5 * Short.BYTES;
        ByteBuffer wav = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(36 + pcmBytes);
        wav.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        wav.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(16);
        wav.putShort((short) 1);
        wav.putShort((short) 1);
        wav.putInt(sampleRate);
        wav.putInt(sampleRate * Short.BYTES);
        wav.putShort((short) Short.BYTES);
        wav.putShort((short) 16);
        wav.put("data".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(pcmBytes);
        return wav.array();
    }
}
