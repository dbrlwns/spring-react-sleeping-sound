package com.example.sleepknowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** 내레이션 adapter에만 필요한 운영 설정입니다. */
@ConfigurationProperties("app.narration")
public record NarrationProperties(String command, Integer baseWordsPerMinute, Duration timeout) {

    public NarrationProperties {
        command = (command == null || command.isBlank()) ? "/usr/bin/say" : command;
        baseWordsPerMinute = baseWordsPerMinute == null ? 180 : baseWordsPerMinute;
        timeout = timeout == null ? Duration.ofMinutes(5) : timeout;

        if (baseWordsPerMinute <= 0) {
            throw new IllegalArgumentException("app.narration.base-words-per-minute must be positive");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("app.narration.timeout must be positive");
        }
    }
}
