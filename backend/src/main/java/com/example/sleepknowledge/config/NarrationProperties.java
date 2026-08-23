package com.example.sleepknowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** composition root가 TTS adapter를 선택하고 구성할 때 사용하는 운영 설정입니다. */
@ConfigurationProperties("app.narration")
public record NarrationProperties(
        Provider provider,
        MacOsSay macosSay,
        GoogleChirp3 googleChirp3
) {

    public NarrationProperties {
        provider = provider == null ? Provider.MACOS_SAY : provider;
        macosSay = macosSay == null ? new MacOsSay(null, null, null) : macosSay;
        googleChirp3 = googleChirp3 == null
                ? new GoogleChirp3(null, null, null, null, null, null, null)
                : googleChirp3;
    }

    public enum Provider {
        MACOS_SAY,
        GOOGLE_CHIRP3
    }

    public record MacOsSay(String command, Integer baseWordsPerMinute, Duration timeout) {

        public MacOsSay {
            command = textOrDefault(command, "/usr/bin/say");
            baseWordsPerMinute = baseWordsPerMinute == null ? 180 : baseWordsPerMinute;
            timeout = timeout == null ? Duration.ofMinutes(5) : timeout;

            if (baseWordsPerMinute <= 0) {
                throw new IllegalArgumentException(
                        "app.narration.macos-say.base-words-per-minute must be positive"
                );
            }
            requirePositive(timeout, "app.narration.macos-say.timeout");
        }
    }

    public record GoogleChirp3(
            String endpoint,
            String languageCode,
            String voiceName,
            Integer maxInputBytes,
            Integer maxChunks,
            Integer maxAudioBytes,
            Duration rpcTimeout
    ) {

        public static final int CLOUD_TTS_MAX_INPUT_BYTES = 5_000;

        public GoogleChirp3 {
            endpoint = textOrDefault(endpoint, "texttospeech.googleapis.com:443");
            languageCode = textOrDefault(languageCode, "ko-KR");
            voiceName = textOrDefault(voiceName, "ko-KR-Chirp3-HD-Kore");
            maxInputBytes = maxInputBytes == null ? CLOUD_TTS_MAX_INPUT_BYTES : maxInputBytes;
            maxChunks = maxChunks == null ? 32 : maxChunks;
            maxAudioBytes = maxAudioBytes == null ? 256 * 1024 * 1024 : maxAudioBytes;
            rpcTimeout = rpcTimeout == null ? Duration.ofSeconds(30) : rpcTimeout;

            if (maxInputBytes < 1 || maxInputBytes > CLOUD_TTS_MAX_INPUT_BYTES) {
                throw new IllegalArgumentException(
                        "app.narration.google-chirp3.max-input-bytes must be between 1 and 5000"
                );
            }
            if (maxChunks <= 0) {
                throw new IllegalArgumentException("app.narration.google-chirp3.max-chunks must be positive");
            }
            if (maxAudioBytes <= 0 || maxAudioBytes > Integer.MAX_VALUE - 44) {
                throw new IllegalArgumentException(
                        "app.narration.google-chirp3.max-audio-bytes is outside the supported WAV range"
                );
            }
            String expectedVoicePrefix = languageCode + "-Chirp3-HD-";
            if (!voiceName.regionMatches(true, 0, expectedVoicePrefix, 0, expectedVoicePrefix.length())) {
                throw new IllegalArgumentException(
                        "app.narration.google-chirp3.voice-name must match the configured language-code"
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
