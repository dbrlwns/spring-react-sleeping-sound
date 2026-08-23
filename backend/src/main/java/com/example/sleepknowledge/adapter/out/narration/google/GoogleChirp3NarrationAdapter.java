package com.example.sleepknowledge.adapter.out.narration.google;

import com.example.sleepknowledge.application.exception.SpeechSynthesisException;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.config.NarrationProperties;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;

import java.util.ArrayList;
import java.util.List;

/** Google Cloud의 동기 Chirp 3 HD 합성을 SpeechSynthesisPort로 번역합니다. */
public final class GoogleChirp3NarrationAdapter implements SpeechSynthesisPort {

    private static final String VOICE_DESCRIPTION = "Google Cloud Chirp 3 HD";

    private final TextToSpeechClient client;
    private final NarrationProperties.GoogleChirp3 properties;
    private final Voice configuredVoice;

    public GoogleChirp3NarrationAdapter(
            TextToSpeechClient client,
            NarrationProperties.GoogleChirp3 properties
    ) {
        this.client = client;
        this.properties = properties;
        this.configuredVoice = new Voice(
                properties.voiceName(),
                properties.voiceName(),
                VOICE_DESCRIPTION,
                properties.languageCode()
        );
    }

    @Override
    public List<Voice> findAvailableVoices() {
        // 설정된 voice만 반환해 자동 생성마다 별도의 ListVoices RPC를 호출하지 않는다.
        return List.of(configuredVoice);
    }

    @Override
    public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
        if (!configuredVoice.id().equals(voice.id()) || !configuredVoice.id().equals(options.voiceId())) {
            throw new SpeechSynthesisException("설정된 Chirp 3 음성과 요청 음성이 일치하지 않습니다.");
        }

        final List<String> chunks;
        try {
            chunks = Utf8TextChunker.split(script, properties.maxInputBytes());
        } catch (IllegalArgumentException exception) {
            throw new SpeechSynthesisException("원고를 Cloud TTS 요청 크기에 맞게 나누지 못했습니다.", exception);
        }
        if (chunks.size() > properties.maxChunks()) {
            throw new SpeechSynthesisException("원고가 한 번에 생성할 수 있는 Cloud TTS 요청 수를 초과했습니다.");
        }

        VoiceSelectionParams voiceSelection = VoiceSelectionParams.newBuilder()
                .setLanguageCode(properties.languageCode())
                .setName(properties.voiceName())
                .build();
        AudioConfig audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.LINEAR16)
                .setSpeakingRate(options.speed())
                .build();

        List<byte[]> wavFiles = new ArrayList<>(chunks.size());
        long receivedAudioBytes = 0;
        for (String chunk : chunks) {
            byte[] wav = synthesizeChunk(chunk, voiceSelection, audioConfig);
            try {
                receivedAudioBytes += Linear16Wav.dataSize(wav);
            } catch (IllegalArgumentException exception) {
                throw new SpeechSynthesisException(
                        "Cloud TTS가 유효한 LINEAR16 WAV를 반환하지 않았습니다.",
                        exception
                );
            }
            if (receivedAudioBytes > properties.maxAudioBytes()) {
                throw new SpeechSynthesisException("Cloud TTS 오디오가 허용된 크기를 초과했습니다.");
            }
            wavFiles.add(wav);
        }

        try {
            return AudioContent.wav(Linear16Wav.merge(wavFiles, properties.maxAudioBytes()));
        } catch (IllegalArgumentException exception) {
            throw new SpeechSynthesisException("Cloud TTS가 유효한 LINEAR16 WAV를 반환하지 않았습니다.", exception);
        }
    }

    private byte[] synthesizeChunk(
            String chunk,
            VoiceSelectionParams voiceSelection,
            AudioConfig audioConfig
    ) {
        try {
            SynthesisInput input = SynthesisInput.newBuilder().setText(chunk).build();
            return client.synthesizeSpeech(input, voiceSelection, audioConfig)
                    .getAudioContent()
                    .toByteArray();
        } catch (ApiException exception) {
            // exception 원문은 로그의 cause로만 남기고, 공개 status에는 고정 메시지만 저장한다.
            throw new SpeechSynthesisException("Google Cloud TTS 요청에 실패했습니다.", exception);
        } catch (RuntimeException exception) {
            throw new SpeechSynthesisException("Google Cloud TTS 응답을 처리하지 못했습니다.", exception);
        }
    }
}
