package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.Voice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-narration-repository-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JpaNarrationAssetRepositoryIntegrationTest {

    private static final Voice YUNA = new Voice("Yuna", "Yuna", "한국어", "ko_KR");
    private static final NarrationOptions OPTIONS = new NarrationOptions("Yuna", 0.9);
    private static final Instant SOURCE_TIME = Instant.parse("2026-08-23T08:00:00Z");
    private static final Instant STATUS_TIME = Instant.parse("2026-08-23T08:00:01Z");

    private final NarrationAssetRepositoryPort repository;

    @Autowired
    JpaNarrationAssetRepositoryIntegrationTest(NarrationAssetRepositoryPort repository) {
        this.repository = repository;
    }

    @Test
    void ready_wav와_상태를_H2에_저장하고_각각_조회한다() {
        UUID contentId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        repository.resetToPending(contentId, generationId, SOURCE_TIME, STATUS_TIME);

        assertThat(repository.markProcessing(contentId, generationId, STATUS_TIME.plusSeconds(1))).isTrue();
        assertThat(repository.markReady(
                contentId,
                generationId,
                YUNA,
                OPTIONS,
                AudioContent.wav(new byte[]{82, 73, 70, 70}),
                STATUS_TIME.plusSeconds(2)
        )).isTrue();

        assertThat(repository.findState(contentId).orElseThrow().status()).isEqualTo(NarrationStatus.READY);
        assertThat(repository.findReadyAudio(contentId).orElseThrow().bytes())
                .containsExactly(82, 73, 70, 70);
    }

    @Test
    void 새_generation으로_reset한_뒤에는_이전_generation이_완료를_덮어쓸_수_없다() {
        UUID contentId = UUID.randomUUID();
        UUID oldGeneration = UUID.randomUUID();
        UUID newGeneration = UUID.randomUUID();
        repository.resetToPending(contentId, oldGeneration, SOURCE_TIME, STATUS_TIME);
        assertThat(repository.markProcessing(contentId, oldGeneration, STATUS_TIME.plusSeconds(1))).isTrue();

        repository.resetToPending(
                contentId,
                newGeneration,
                SOURCE_TIME.plusSeconds(10),
                STATUS_TIME.plusSeconds(10)
        );
        boolean staleWrite = repository.markReady(
                contentId,
                oldGeneration,
                YUNA,
                OPTIONS,
                AudioContent.wav(new byte[]{9, 9, 9}),
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
}
