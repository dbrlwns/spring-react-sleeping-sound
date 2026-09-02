package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.port.out.AudioTranscodingPort;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.example.sleepknowledge.domain.model.Voice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NarrationGenerationWorkerTest {

    private static final Instant SOURCE_UPDATED_AT = Instant.parse("2026-08-20T11:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private final UUID contentId = UUID.randomUUID();
    private final UUID generationId = UUID.randomUUID();
    private final Voice selectedVoice = NarrationVoiceOption.SCHEDAR.toVoice();
    private final Episode episode = new Episode(
            contentId, "별 이야기", "요약", ContentCategory.ASTRONOMY,
            "관리자가 승인한 원고입니다.", SOURCE_UPDATED_AT, SOURCE_UPDATED_AT
    );
    private ContentRepositoryPort contents;
    private NarrationAssetRepositoryPort narrations;
    private SpeechSynthesisPort speech;
    private AudioTranscodingPort transcoder;
    private NarrationGenerationWorker worker;

    @BeforeEach
    void setUp() {
        contents = mock(ContentRepositoryPort.class);
        narrations = mock(NarrationAssetRepositoryPort.class);
        speech = mock(SpeechSynthesisPort.class);
        transcoder = mock(AudioTranscodingPort.class);
        when(narrations.markProcessing(contentId, generationId, NOW)).thenReturn(true);
        when(narrations.findState(contentId)).thenReturn(Optional.of(new NarrationState(
                contentId, generationId, SOURCE_UPDATED_AT, NarrationStatus.PROCESSING,
                selectedVoice.id(), null, NOW, false
        )));
        when(contents.findById(contentId)).thenReturn(Optional.of(episode));
        when(speech.findAvailableVoices()).thenReturn(NarrationVoiceOption.voices());
        when(speech.synthesize(any(), any(), any()))
                .thenReturn(AudioContent.linear16Wav(new byte[]{82, 73, 70, 70}));
        when(transcoder.encodeMp3(any()))
                .thenReturn(AudioContent.mp3(new byte[]{(byte) 0xff, (byte) 0xfb, (byte) 0x90, 0x64}));
        worker = new NarrationGenerationWorker(
                contents, narrations, speech, transcoder, Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void event에_고정된_관리자_선택_voice로만_합성한다() {
        worker.generate(request());

        ArgumentCaptor<NarrationOptions> options = ArgumentCaptor.forClass(NarrationOptions.class);
        ArgumentCaptor<AudioContent> storedAudio = ArgumentCaptor.forClass(AudioContent.class);
        verify(transcoder).verifyAvailable();
        verify(speech).synthesize(eq(episode.script()), options.capture(), eq(selectedVoice));
        verify(transcoder).encodeMp3(any(AudioContent.class));
        assertThat(options.getValue().voiceId()).isEqualTo(selectedVoice.id());
        assertThat(options.getValue().speed()).isEqualTo(0.9);
        verify(narrations).markReady(
                eq(contentId), eq(generationId), eq(selectedVoice),
                eq(options.getValue()), storedAudio.capture(), eq(NOW)
        );
        assertThat(storedAudio.getValue().mediaType()).isEqualTo(AudioContent.MP3_MEDIA_TYPE);
    }

    @Test
    void 승인_voice가_공급자에_없으면_합성하지_않고_failed로_기록한다() {
        when(speech.findAvailableVoices()).thenReturn(List.of(NarrationVoiceOption.KORE.toVoice()));

        worker.generate(request());

        verify(speech, never()).synthesize(any(), any(), any());
        verify(narrations).markFailed(
                eq(contentId), eq(generationId),
                eq("승인된 TTS 음성을 사용할 수 없습니다."), eq(NOW)
        );
    }

    @Test
    void FFmpeg를_실행할_수_없으면_유료_TTS_호출_전에_failed로_기록한다() {
        org.mockito.Mockito.doThrow(new IllegalStateException("FFmpeg unavailable"))
                .when(transcoder).verifyAvailable();

        worker.generate(request());

        verify(speech, never()).findAvailableVoices();
        verify(speech, never()).synthesize(any(), any(), any());
        verify(narrations).markFailed(
                eq(contentId), eq(generationId), eq("FFmpeg unavailable"), eq(NOW)
        );
        verify(transcoder, never()).encodeMp3(any());
    }

    @Test
    void MP3_인코딩에_실패하면_ready를_저장하지_않고_failed로_기록한다() {
        when(transcoder.encodeMp3(any())).thenThrow(new IllegalStateException("MP3 encode failed"));

        worker.generate(request());

        verify(speech).synthesize(any(), any(), any());
        verify(narrations, never()).markReady(any(), any(), any(), any(), any(), any());
        verify(narrations).markFailed(
                eq(contentId), eq(generationId), eq("MP3 encode failed"), eq(NOW)
        );
    }

    @Test
    void 원고_수정으로_generation_row가_삭제되면_유료_TTS_호출_전에_중단한다() {
        when(narrations.findState(contentId)).thenReturn(Optional.empty());

        worker.generate(request());

        verify(speech, never()).findAvailableVoices();
        verify(speech, never()).synthesize(any(), any(), any());
        verify(narrations, never()).markReady(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 이미_다른_worker가_generation을_선점했으면_아무것도_하지_않는다() {
        when(narrations.markProcessing(contentId, generationId, NOW)).thenReturn(false);

        worker.generate(request());

        verify(contents, never()).findById(any());
        verify(speech, never()).synthesize(any(), any(), any());
    }

    private NarrationGenerationRequested request() {
        return new NarrationGenerationRequested(
                contentId, generationId, SOURCE_UPDATED_AT, selectedVoice.id()
        );
    }
}
