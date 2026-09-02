package com.example.sleepknowledge.config;

import com.example.sleepknowledge.adapter.out.narration.google.GoogleChirp3NarrationAdapter;
import com.example.sleepknowledge.adapter.out.narration.ffmpeg.FfmpegMp3AudioTranscoder;
import com.example.sleepknowledge.application.port.out.AudioTranscodingPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Google Chirp3 TTS client와 outbound adapter를 조립하는 composition root입니다. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class NarrationConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(TextToSpeechClient.class)
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
    SpeechSynthesisPort googleChirp3SpeechSynthesisPort(
            TextToSpeechClient client,
            NarrationProperties properties
    ) {
        return new GoogleChirp3NarrationAdapter(client, properties.googleChirp3());
    }

    @Bean
    @ConditionalOnMissingBean(AudioTranscodingPort.class)
    AudioTranscodingPort ffmpegMp3AudioTranscodingPort(NarrationProperties properties) {
        return new FfmpegMp3AudioTranscoder(properties.ffmpeg());
    }

    /** 큰 WAV byte array가 동시에 여러 개 생기지 않도록 내레이션 전체 작업을 직렬화합니다. */
    @Bean(destroyMethod = "shutdown")
    ExecutorService narrationTaskExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "narration-worker");
            thread.setDaemon(true);
            return thread;
        });
    }
}
