package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.example.sleepknowledge.domain.model.Voice;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-narration-repository-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JpaNarrationAssetRepositoryIntegrationTest {

    @MockitoBean
    private TextToSpeechClient textToSpeechClient;

    private static final Voice KORE = NarrationVoiceOption.KORE.toVoice();
    private static final NarrationOptions OPTIONS = new NarrationOptions(KORE.id(), 0.9);
    private static final Instant SOURCE_TIME = Instant.parse("2026-08-23T08:00:00Z");
    private static final Instant STATUS_TIME = Instant.parse("2026-08-23T08:00:01Z");

    private final NarrationAssetRepositoryPort repository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JpaNarrationAssetRepositoryIntegrationTest(
            NarrationAssetRepositoryPort repository,
            JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void ready_mp3와_media_type을_H2에_저장하고_각각_조회한다() {
        UUID contentId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        repository.resetToPending(contentId, generationId, SOURCE_TIME, KORE.id(), STATUS_TIME);

        assertThat(repository.markProcessing(contentId, generationId, STATUS_TIME.plusSeconds(1))).isTrue();
        assertThat(repository.markReady(
                contentId,
                generationId,
                KORE,
                OPTIONS,
                AudioContent.mp3(new byte[]{(byte) 0xff, (byte) 0xfb, (byte) 0x90, 0x64}),
                STATUS_TIME.plusSeconds(2)
        )).isTrue();

        assertThat(repository.findState(contentId).orElseThrow().status()).isEqualTo(NarrationStatus.READY);
        assertThat(repository.findReadyAudio(contentId).orElseThrow().bytes())
                .containsExactly((byte) 0xff, (byte) 0xfb, (byte) 0x90, (byte) 0x64);
        assertThat(jdbcTemplate.queryForObject(
                "select media_type from narration_assets where content_id = ?",
                String.class,
                contentId
        )).isEqualTo(AudioContent.MP3_MEDIA_TYPE);
    }

    @Test
    void 새_generation으로_reset한_뒤에는_이전_generation이_완료를_덮어쓸_수_없다() {
        UUID contentId = UUID.randomUUID();
        UUID oldGeneration = UUID.randomUUID();
        UUID newGeneration = UUID.randomUUID();
        repository.resetToPending(contentId, oldGeneration, SOURCE_TIME, KORE.id(), STATUS_TIME);
        assertThat(repository.markProcessing(contentId, oldGeneration, STATUS_TIME.plusSeconds(1))).isTrue();

        repository.resetToPending(
                contentId,
                newGeneration,
                SOURCE_TIME.plusSeconds(10),
                KORE.id(),
                STATUS_TIME.plusSeconds(10)
        );
        boolean staleWrite = repository.markReady(
                contentId,
                oldGeneration,
                KORE,
                OPTIONS,
                AudioContent.mp3(new byte[]{(byte) 0xff, (byte) 0xfb, 9, 9}),
                STATUS_TIME.plusSeconds(11)
        );

        assertThat(staleWrite).isFalse();
        assertThat(repository.findState(contentId).orElseThrow())
                .satisfies(state -> {
                    assertThat(state.generationId()).isEqualTo(newGeneration);
                    assertThat(state.status()).isEqualTo(NarrationStatus.PENDING);
                    assertThat(state.audioAvailable()).isFalse();
                });
        assertThat(repository.findReadyAudio(contentId)).isEmpty();
    }

    @Test
    void WAV는_ready_asset으로_저장하지_않는다() {
        UUID contentId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        repository.resetToPending(contentId, generationId, SOURCE_TIME, KORE.id(), STATUS_TIME);
        assertThat(repository.markProcessing(contentId, generationId, STATUS_TIME.plusSeconds(1))).isTrue();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.markReady(
                contentId,
                generationId,
                KORE,
                OPTIONS,
                AudioContent.linear16Wav(new byte[]{82, 73, 70, 70}),
                STATUS_TIME.plusSeconds(2)
        )).isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audio/mpeg");

        assertThat(repository.findReadyAudio(contentId)).isEmpty();
    }

    @Test
    void 이전_버전의_voice가_없는_pending_row는_NOT_REQUESTED로_호환한다() {
        UUID contentId = UUID.randomUUID();
        jdbcTemplate.update("""
                        insert into narration_assets (
                            content_id, generation_id, source_updated_at, status, voice_id, speed,
                            audio_available, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                contentId,
                UUID.randomUUID(),
                SOURCE_TIME,
                "PENDING",
                null,
                0.9,
                false,
                STATUS_TIME
        );

        assertThat(repository.findState(contentId).orElseThrow().status())
                .isEqualTo(NarrationStatus.NOT_REQUESTED);
    }
}
