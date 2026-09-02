package com.example.sleepknowledge.adapter.out.narration.google;

import com.example.sleepknowledge.application.exception.SpeechSynthesisException;
import com.example.sleepknowledge.config.NarrationProperties;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GoogleChirp3NarrationAdapterTest {

    private final TextToSpeechClient client = mock(TextToSpeechClient.class);

    @Test
    void 관리자_허용_voice_여섯_종은_RPC없이_제공한다() {
        var adapter = new GoogleChirp3NarrationAdapter(client, properties(5_000, 32, 1_000));

        var voices = adapter.findAvailableVoices();

        assertThat(voices).hasSize(6)
                .extracting("id")
                .containsExactly(
                        "ko-KR-Chirp3-HD-Kore",
                        "ko-KR-Chirp3-HD-Zephyr",
                        "ko-KR-Chirp3-HD-Leda",
                        "ko-KR-Chirp3-HD-Charon",
                        "ko-KR-Chirp3-HD-Schedar",
                        "ko-KR-Chirp3-HD-Achird"
                );
        verifyNoInteractions(client);
    }

    @Test
    void plain_text를_chunk별_LINEAR16_동기_요청하고_유효한_WAV로_합친다() {
        byte[] responseWav = WavTestFixtures.wav(24_000, new byte[]{1, 2}, false, false);
        when(client.synthesizeSpeech(
                any(SynthesisInput.class),
                any(VoiceSelectionParams.class),
                any(AudioConfig.class)
        )).thenReturn(response(responseWav));
        var adapter = new GoogleChirp3NarrationAdapter(client, properties(12, 32, 1_000));
        var voice = adapter.findAvailableVoices().get(5);
        String script = "한글 문장과 emoji 😊를 읽습니다.";

        var audio = adapter.synthesize(script, new NarrationOptions(voice.id(), 0.9d), voice);

        ArgumentCaptor<SynthesisInput> inputCaptor = ArgumentCaptor.forClass(SynthesisInput.class);
        ArgumentCaptor<VoiceSelectionParams> voiceCaptor = ArgumentCaptor.forClass(VoiceSelectionParams.class);
        ArgumentCaptor<AudioConfig> audioCaptor = ArgumentCaptor.forClass(AudioConfig.class);
        org.mockito.Mockito.verify(client, org.mockito.Mockito.atLeast(2)).synthesizeSpeech(
                inputCaptor.capture(),
                voiceCaptor.capture(),
                audioCaptor.capture()
        );
        assertThat(inputCaptor.getAllValues())
                .allSatisfy(input -> {
                    assertThat(input.getInputSourceCase()).isEqualTo(SynthesisInput.InputSourceCase.TEXT);
                    assertThat(input.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                            .isLessThanOrEqualTo(12);
                });
        assertThat(inputCaptor.getAllValues().stream().map(SynthesisInput::getText).collect(
                java.util.stream.Collectors.joining()
        )).isEqualTo(script);
        assertThat(voiceCaptor.getAllValues()).allSatisfy(selection -> {
            assertThat(selection.getLanguageCode()).isEqualTo("ko-KR");
            assertThat(selection.getName()).isEqualTo("ko-KR-Chirp3-HD-Achird");
        });
        assertThat(audioCaptor.getAllValues()).allSatisfy(config -> {
            assertThat(config.getAudioEncoding()).isEqualTo(AudioEncoding.LINEAR16);
            assertThat(config.getSpeakingRate()).isEqualTo(0.9d);
        });
        assertThat(audio.mediaType()).isEqualTo(AudioContent.LINEAR16_WAV_MEDIA_TYPE);
        assertThat(java.util.Arrays.copyOfRange(audio.bytes(), 0, 4))
                .containsExactly((byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F');
        assertThat(audio.bytes().length).isEqualTo(44 + inputCaptor.getAllValues().size() * 2);
    }

    @Test
    void SDK_상세_오류를_공개해도_안전한_고정_메시지로_변환한다() {
        ApiException sdkException = mock(ApiException.class);
        when(sdkException.getMessage()).thenReturn("credential=/secret/key.json project=my-project");
        when(client.synthesizeSpeech(
                any(SynthesisInput.class),
                any(VoiceSelectionParams.class),
                any(AudioConfig.class)
        )).thenThrow(sdkException);
        var adapter = new GoogleChirp3NarrationAdapter(client, properties(5_000, 32, 1_000));
        var voice = adapter.findAvailableVoices().get(0);

        assertThatThrownBy(() -> adapter.synthesize("안전한 원고", new NarrationOptions(voice.id(), 0.9d), voice))
                .isInstanceOf(SpeechSynthesisException.class)
                .hasMessage("Google Cloud TTS 요청에 실패했습니다.")
                .hasMessageNotContaining("secret")
                .hasCause(sdkException);
    }

    @Test
    void 유효하지_않은_WAV와_과도한_chunk_수를_안전하게_거절한다() {
        when(client.synthesizeSpeech(
                any(SynthesisInput.class),
                any(VoiceSelectionParams.class),
                any(AudioConfig.class)
        )).thenReturn(response(new byte[]{1, 2, 3}));
        var invalidWavAdapter = new GoogleChirp3NarrationAdapter(client, properties(5_000, 32, 1_000));
        var voice = invalidWavAdapter.findAvailableVoices().get(0);

        assertThatThrownBy(() -> invalidWavAdapter.synthesize(
                "원고",
                new NarrationOptions(voice.id(), 0.9d),
                voice
        )).isInstanceOf(SpeechSynthesisException.class)
                .hasMessage("Cloud TTS가 유효한 LINEAR16 WAV를 반환하지 않았습니다.");

        TextToSpeechClient untouchedClient = mock(TextToSpeechClient.class);
        var tooManyChunksAdapter = new GoogleChirp3NarrationAdapter(
                untouchedClient,
                properties(4, 1, 1_000)
        );
        var configuredVoice = tooManyChunksAdapter.findAvailableVoices().get(0);
        assertThatThrownBy(() -> tooManyChunksAdapter.synthesize(
                "가나다",
                new NarrationOptions(configuredVoice.id(), 0.9d),
                configuredVoice
        )).isInstanceOf(SpeechSynthesisException.class)
                .hasMessageContaining("요청 수를 초과");
        verifyNoInteractions(untouchedClient);
    }

    private static NarrationProperties.GoogleChirp3 properties(
            int maxInputBytes,
            int maxChunks,
            int maxAudioBytes
    ) {
        return new NarrationProperties.GoogleChirp3(
                "texttospeech.googleapis.com:443",
                maxInputBytes,
                maxChunks,
                maxAudioBytes,
                Duration.ofSeconds(10)
        );
    }

    private static SynthesizeSpeechResponse response(byte[] wav) {
        return SynthesizeSpeechResponse.newBuilder()
                .setAudioContent(ByteString.copyFrom(wav))
                .build();
    }
}
