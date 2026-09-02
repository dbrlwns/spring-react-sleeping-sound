package com.example.sleepknowledge.config;

import com.example.sleepknowledge.adapter.out.narration.google.GoogleChirp3NarrationAdapter;
import com.example.sleepknowledge.adapter.out.narration.ffmpeg.FfmpegMp3AudioTranscoder;
import com.example.sleepknowledge.application.port.out.AudioTranscodingPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NarrationConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NarrationConfiguration.class);

    @Test
    void Google_client로_Chirp3_adapter를_조립한다() {
        TextToSpeechClient client = mock(TextToSpeechClient.class);
        var properties = new NarrationProperties(null, null);

        contextRunner
                .withBean(NarrationProperties.class, () -> properties)
                .withBean(TextToSpeechClient.class, () -> client)
                .run(context -> {
                    assertThat(context).hasSingleBean(SpeechSynthesisPort.class);
                    assertThat(context.getBean(SpeechSynthesisPort.class))
                            .isInstanceOf(GoogleChirp3NarrationAdapter.class);
                    assertThat(context).hasSingleBean(TextToSpeechClient.class);
                    assertThat(context).hasSingleBean(AudioTranscodingPort.class);
                    assertThat(context.getBean(AudioTranscodingPort.class))
                            .isInstanceOf(FfmpegMp3AudioTranscoder.class);
                    assertThat(context).hasBean("narrationTaskExecutor");
                });
    }
}
