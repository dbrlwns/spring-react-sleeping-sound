package com.example.sleepknowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 시간처럼 환경에 따라 달라지는 의존성도 주입해 유스케이스를 결정적으로 테스트합니다. */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
