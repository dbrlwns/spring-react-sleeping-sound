package com.example.sleepknowledge.config;

import com.example.sleepknowledge.adapter.out.narration.google.GoogleChirp3NarrationAdapter;
import com.example.sleepknowledge.adapter.out.narration.macos.MacOsSayNarrationAdapter;
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
    void provider가_없으면_Google_client나_ADC없이_macos_adapter만_생성한다() {
        contextRunner
                .withBean(NarrationProperties.class, () -> new NarrationProperties(null, null, null))
                .run(context -> {
                    assertThat(context).hasSingleBean(SpeechSynthesisPort.class);
                    assertThat(context.getBean(SpeechSynthesisPort.class))
                            .isInstanceOf(MacOsSayNarrationAdapter.class);
                    assertThat(context).doesNotHaveBean(TextToSpeechClient.class);
                });
    }

    @Test
    void google_chirp3를_선택한_경우에만_Google_adapter를_조립한다() {
        TextToSpeechClient client = mock(TextToSpeechClient.class);
        var properties = new NarrationProperties(
                NarrationProperties.Provider.GOOGLE_CHIRP3,
                null,
                null
        );

        contextRunner
                .withPropertyValues("app.narration.provider=google-chirp3")
                .withBean(NarrationProperties.class, () -> properties)
                .withBean(TextToSpeechClient.class, () -> client)
                .run(context -> {
                    assertThat(context).hasSingleBean(SpeechSynthesisPort.class);
                    assertThat(context.getBean(SpeechSynthesisPort.class))
                            .isInstanceOf(GoogleChirp3NarrationAdapter.class);
                    assertThat(context).hasSingleBean(TextToSpeechClient.class);
                });
    }
}
