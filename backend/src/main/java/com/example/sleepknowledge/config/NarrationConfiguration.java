package com.example.sleepknowledge.config;

import com.example.sleepknowledge.adapter.out.narration.macos.MacOsSayNarrationAdapter;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** TTS 공급자를 바꿀 때 교체할 composition root입니다. */
@Configuration(proxyBeanMethods = false)
public class NarrationConfiguration {

    @Bean
    SpeechSynthesisPort speechSynthesisPort(NarrationProperties properties) {
        return new MacOsSayNarrationAdapter(properties);
    }
}
