package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.exception.NarrationProposalAlreadyPendingException;
import com.example.sleepknowledge.application.exception.NarrationProposalConflictException;
import com.example.sleepknowledge.application.port.out.NarrationProposalRepositoryPort;
import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-proposal-repository-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JpaNarrationProposalRepositoryIntegrationTest {

    @MockitoBean
    private TextToSpeechClient textToSpeechClient;

    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");
    private final NarrationProposalRepositoryPort repository;

    @Autowired
    JpaNarrationProposalRepositoryIntegrationTest(NarrationProposalRepositoryPort repository) {
        this.repository = repository;
    }

    @Test
    void DB_unique_constraint가_동일_콘텐츠의_PENDING_중복을_막는다() {
        UUID contentId = UUID.randomUUID();
        repository.createPending(pending(contentId, "reader-1", NOW));

        assertThatThrownBy(() -> repository.createPending(pending(
                contentId, "reader-2", NOW.plusSeconds(1)
        ))).isInstanceOf(NarrationProposalAlreadyPendingException.class);
    }

    @Test
    void 승인된_제안은_다시_결정할_수_없고_새_제안은_받을_수_있다() {
        UUID contentId = UUID.randomUUID();
        NarrationProposal pending = repository.createPending(pending(contentId, "reader", NOW));
        NarrationProposal approved = repository.approvePending(
                pending.id(), NarrationVoiceOption.LEDA.voiceId(), "admin", NOW.plusSeconds(1)
        );

        assertThat(approved.status()).isEqualTo(NarrationProposalStatus.APPROVED);
        assertThat(approved.preferredVoiceId()).isEqualTo(NarrationVoiceOption.KORE.voiceId());
        assertThat(approved.selectedVoiceId()).isEqualTo(NarrationVoiceOption.LEDA.voiceId());
        assertThatThrownBy(() -> repository.rejectPending(
                pending.id(), "other-admin", NOW.plusSeconds(2)
        )).isInstanceOf(NarrationProposalConflictException.class);

        NarrationProposal next = repository.createPending(pending(
                contentId, "reader-2", NOW
        ));
        assertThat(next.status()).isEqualTo(NarrationProposalStatus.PENDING);
        assertThat(repository.findLatestByContentId(contentId).orElseThrow().id()).isEqualTo(next.id());
    }

    @Test
    void 이전_제안_row의_nullable_희망_voice를_읽고_승인할_수_있다() {
        UUID contentId = UUID.randomUUID();
        NarrationProposal legacy = repository.createPending(NarrationProposal.pending(
                UUID.randomUUID(), contentId, "legacy-reader", NOW
        ));

        assertThat(legacy.preferredVoiceId()).isNull();
        assertThat(repository.findLatestByContentId(contentId).orElseThrow().preferredVoiceId()).isNull();

        NarrationProposal approved = repository.approvePending(
                legacy.id(), NarrationVoiceOption.ACHIRD.voiceId(), "admin", NOW.plusSeconds(1)
        );
        assertThat(approved.preferredVoiceId()).isNull();
        assertThat(approved.selectedVoiceId()).isEqualTo(NarrationVoiceOption.ACHIRD.voiceId());
    }

    private NarrationProposal pending(UUID contentId, String requestedBy, Instant requestedAt) {
        return NarrationProposal.pending(
                UUID.randomUUID(), contentId, requestedBy, requestedAt,
                NarrationVoiceOption.KORE.voiceId()
        );
    }
}
