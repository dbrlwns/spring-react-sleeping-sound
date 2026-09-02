package com.example.sleepknowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** composition root가 Google Chirp3 TTS adapter를 구성할 때 사용하는 운영 설정입니다. */
@ConfigurationProperties("app.narration")
public record NarrationProperties(
        GoogleChirp3 googleChirp3,
        Ffmpeg ffmpeg
) {

    public NarrationProperties {
        googleChirp3 = googleChirp3 == null
                ? new GoogleChirp3(null, null, null, null, null)
                : googleChirp3;
        ffmpeg = ffmpeg == null
                ? new Ffmpeg(null, null, null, null)
                : ffmpeg;
    }

    public record Ffmpeg(
            String command,
            Duration timeout,
            Integer maxOutputBytes,
            Integer bitrateKbps
    ) {

        public static final int MAX_BITRATE_KBPS = 320;
        public static final int MAX_OUTPUT_BYTES = 512 * 1024 * 1024;
        public static final Duration MAX_TIMEOUT = Duration.ofHours(1);

        public Ffmpeg {
            command = textOrDefault(command, "ffmpeg");
            timeout = timeout == null ? Duration.ofMinutes(5) : timeout;
            maxOutputBytes = maxOutputBytes == null ? 64 * 1024 * 1024 : maxOutputBytes;
            bitrateKbps = bitrateKbps == null ? 64 : bitrateKbps;

            requirePositive(timeout, "app.narration.ffmpeg.timeout");
            if (timeout.compareTo(MAX_TIMEOUT) > 0) {
                throw new IllegalArgumentException("app.narration.ffmpeg.timeout must not exceed 1 hour");
            }
            if (maxOutputBytes <= 0 || maxOutputBytes > MAX_OUTPUT_BYTES) {
                throw new IllegalArgumentException(
                        "app.narration.ffmpeg.max-output-bytes must be between 1 and 536870912"
                );
            }
            if (bitrateKbps < 8 || bitrateKbps > MAX_BITRATE_KBPS) {
                throw new IllegalArgumentException(
                        "app.narration.ffmpeg.bitrate-kbps must be between 8 and 320"
                );
            }
        }
    }

    public record GoogleChirp3(
            String endpoint,
            Integer maxInputBytes,
            Integer maxChunks,
            Integer maxAudioBytes,
            Duration rpcTimeout
    ) {

        public static final int CLOUD_TTS_MAX_INPUT_BYTES = 5_000;
        public static final int MIN_UTF8_CODE_POINT_BYTES = 4;
        public static final int MAX_CHUNKS = 64;

        public GoogleChirp3 {
            endpoint = textOrDefault(endpoint, "texttospeech.googleapis.com:443");
            maxInputBytes = maxInputBytes == null ? CLOUD_TTS_MAX_INPUT_BYTES : maxInputBytes;
            maxChunks = maxChunks == null ? 32 : maxChunks;
            maxAudioBytes = maxAudioBytes == null ? 128 * 1024 * 1024 : maxAudioBytes;
            rpcTimeout = rpcTimeout == null ? Duration.ofMinutes(30) : rpcTimeout;

            if (maxInputBytes < MIN_UTF8_CODE_POINT_BYTES || maxInputBytes > CLOUD_TTS_MAX_INPUT_BYTES) {
                throw new IllegalArgumentException(
                        "app.narration.google-chirp3.max-input-bytes must be between 4 and 5000"
                );
            }
            if (maxChunks <= 0 || maxChunks > MAX_CHUNKS) {
                throw new IllegalArgumentException(
                        "app.narration.google-chirp3.max-chunks must be between 1 and 64"
                );
            }
            if (maxAudioBytes <= 0 || maxAudioBytes > Integer.MAX_VALUE - 44) {
                throw new IllegalArgumentException(
                        "app.narration.google-chirp3.max-audio-bytes is outside the supported WAV range"
                );
            }
            requirePositive(rpcTimeout, "app.narration.google-chirp3.rpc-timeout");
        }
    }

    private static String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static void requirePositive(Duration value, String propertyName) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
    }
}
