package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.exception.NarrationNotReadyException;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NarrationServiceTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-08-20T12:00:00Z");
    private static final String VOICE_ID = NarrationVoiceOption.KORE.voiceId();
    private final UUID contentId = UUID.randomUUID();
    private final Episode episode = new Episode(
            contentId, "우주 이야기", "요약", ContentCategory.COSMOLOGY, "저장된 원고입니다.",
            UPDATED_AT.minusSeconds(10), UPDATED_AT
    );
    private ContentRepositoryPort contents;
    private NarrationAssetRepositoryPort narrations;
    private NarrationService service;

    @BeforeEach
    void setUp() {
        contents = mock(ContentRepositoryPort.class);
        narrations = mock(NarrationAssetRepositoryPort.class);
        when(contents.findById(contentId)).thenReturn(Optional.of(episode));
        service = new NarrationService(contents, narrations);
    }

    @Test
    void asset이_없으면_DB를_변경하지_않고_NOT_REQUESTED를_반환한다() {
        when(narrations.findState(contentId)).thenReturn(Optional.empty());

        NarrationState state = service.getNarrationStatus(contentId);

        assertThat(state.status()).isEqualTo(NarrationStatus.NOT_REQUESTED);
        assertThat(state.generationId()).isNull();
        assertThat(state.selectedVoiceId()).isNull();
        verify(narrations, never()).resetToPending(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void ready_asset은_승인기록과_무관하게_기존_호환성을_유지한다() {
        NarrationState ready = state(NarrationStatus.READY, true, "Yuna");
        byte[] mp3 = {(byte) 0xff, (byte) 0xfb, (byte) 0x90, 0x64};
        when(narrations.findState(contentId)).thenReturn(Optional.of(ready));
        when(narrations.findReadyAudio(contentId)).thenReturn(Optional.of(AudioContent.mp3(mp3)));

        assertThat(service.getNarrationStatus(contentId).status()).isEqualTo(NarrationStatus.READY);
        assertThat(service.getNarrationAudio(contentId).bytes()).containsExactly(mp3);
    }

    @Test
    void 준비되지_않은_audio는_현재_상태로_409_예외를_만든다() {
        when(narrations.findState(contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNarrationAudio(contentId))
                .isInstanceOf(NarrationNotReadyException.class)
                .hasMessageContaining("NOT_REQUESTED");
    }

    @Test
    void 관리자_선택지는_정확히_Chirp3_여섯_종이다() {
        assertThat(service.listVoices())
                .extracting("id")
                .containsExactly(
                        NarrationVoiceOption.KORE.voiceId(),
                        NarrationVoiceOption.ZEPHYR.voiceId(),
                        NarrationVoiceOption.LEDA.voiceId(),
                        NarrationVoiceOption.CHARON.voiceId(),
                        NarrationVoiceOption.SCHEDAR.voiceId(),
                        NarrationVoiceOption.ACHIRD.voiceId()
                );
    }

    private NarrationState state(NarrationStatus status, boolean audioAvailable, String voiceId) {
        return new NarrationState(
                contentId, UUID.randomUUID(), UPDATED_AT, status, voiceId,
                null, UPDATED_AT, audioAvailable
        );
    }
}
