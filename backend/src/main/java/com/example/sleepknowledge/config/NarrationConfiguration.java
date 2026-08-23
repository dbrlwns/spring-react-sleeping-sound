package com.example.sleepknowledge.config;

import com.example.sleepknowledge.adapter.out.narration.google.GoogleChirp3NarrationAdapter;
import com.example.sleepknowledge.adapter.out.narration.macos.MacOsSayNarrationAdapter;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;

/** TTS 공급자를 바꿀 때 교체할 composition root입니다. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class NarrationConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.narration",
            name = "provider",
            havingValue = "macos-say",
            matchIfMissing = true
    )
    SpeechSynthesisPort macOsSaySpeechSynthesisPort(NarrationProperties properties) {
        return new MacOsSayNarrationAdapter(properties.macosSay());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(TextToSpeechClient.class)
    @ConditionalOnProperty(prefix = "app.narration", name = "provider", havingValue = "google-chirp3")
    TextToSpeechClient googleTextToSpeechClient(NarrationProperties properties) throws IOException {
        NarrationProperties.GoogleChirp3 google = properties.googleChirp3();
        TextToSpeechSettings.Builder settings = TextToSpeechSettings.newBuilder()
                .setEndpoint(google.endpoint());
        settings.synthesizeSpeechSettings()
                .setSimpleTimeoutNoRetriesDuration(google.rpcTimeout());

        // 합성은 자동 재시도하지 않으며, create()/close()가 ADC와 채널 생명주기의 경계다.
        return TextToSpeechClient.create(settings.build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.narration", name = "provider", havingValue = "google-chirp3")
    SpeechSynthesisPort googleChirp3SpeechSynthesisPort(
            TextToSpeechClient client,
            NarrationProperties properties
    ) {
        return new GoogleChirp3NarrationAdapter(client, properties.googleChirp3());
    }
}
