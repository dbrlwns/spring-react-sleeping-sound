package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import com.example.sleepknowledge.application.exception.NarrationAlreadySupportedException;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationAssetRepositoryPort;
import com.example.sleepknowledge.application.port.out.NarrationProposalRepositoryPort;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import com.example.sleepknowledge.domain.model.NarrationState;
import com.example.sleepknowledge.domain.model.NarrationStatus;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NarrationProposalServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");
    private final UUID contentId = UUID.randomUUID();
    private final UUID proposalId = UUID.randomUUID();
    private final Episode episode = new Episode(
            contentId, "지식 이야기", "요약", ContentCategory.SOCIETY, "검토할 원고",
            NOW.minusSeconds(10), NOW.minusSeconds(10)
    );
    private ContentRepositoryPort contents;
    private NarrationProposalRepositoryPort proposals;
    private NarrationAssetRepositoryPort narrations;
    private java.util.List<Object> events;
    private NarrationProposalService service;

    @BeforeEach
    void setUp() {
        contents = mock(ContentRepositoryPort.class);
        proposals = mock(NarrationProposalRepositoryPort.class);
        narrations = mock(NarrationAssetRepositoryPort.class);
        events = new java.util.ArrayList<>();
        when(contents.findById(contentId)).thenReturn(Optional.of(episode));
        when(contents.findByIdForUpdate(contentId)).thenReturn(Optional.of(episode));
        service = new NarrationProposalService(
                contents, proposals, narrations, events::add, Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void asset이_없으면_일반_사용자의_PENDING_제안을_저장한다() {
        when(narrations.findState(contentId)).thenReturn(Optional.empty());
        when(proposals.createPending(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String preferredVoiceId = NarrationVoiceOption.LEDA.voiceId();
        NarrationProposal result = service.suggest(contentId, "reader", preferredVoiceId);

        assertThat(result.status()).isEqualTo(NarrationProposalStatus.PENDING);
        assertThat(result.requestedBy()).isEqualTo("reader");
        assertThat(result.requestedAt()).isEqualTo(NOW);
        assertThat(result.preferredVoiceId()).isEqualTo(preferredVoiceId);
    }

    @Test
    void 새_제안_command에서는_희망_voice가_필수다() {
        assertThatThrownBy(() -> service.suggest(contentId, "reader", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voiceId는 필수");
        verify(contents, never()).findById(contentId);
        verify(proposals, never()).createPending(any());
    }

    @Test
    void 희망_voice가_허용목록_밖이면_DB를_조회하기_전에_거절한다() {
        assertThatThrownBy(() -> service.suggest(contentId, "reader", "ko-KR-Chirp3-HD-Puck"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("선택할 수 없는");
        verify(contents, never()).findById(contentId);
        verify(proposals, never()).createPending(any());
    }

    @ParameterizedTest
    @EnumSource(value = NarrationStatus.class, names = {"PENDING", "PROCESSING", "READY"})
    void 이미_음성이_있거나_생성중이면_새_제안을_막는다(NarrationStatus status) {
        when(narrations.findState(contentId)).thenReturn(Optional.of(state(status)));

        assertThatThrownBy(() -> service.suggest(
                contentId, "reader", NarrationVoiceOption.KORE.voiceId()
        ))
                .isInstanceOf(NarrationAlreadySupportedException.class);
        verify(proposals, never()).createPending(any());
    }

    @Test
    void FAILED_asset에는_새_제안을_허용한다() {
        when(narrations.findState(contentId)).thenReturn(Optional.of(state(NarrationStatus.FAILED)));
        when(proposals.createPending(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.suggest(
                contentId, "reader", NarrationVoiceOption.ZEPHYR.voiceId()
        ).status())
                .isEqualTo(NarrationProposalStatus.PENDING);
    }

    @Test
    void 이전_버전_row를_변환한_NOT_REQUESTED에도_새_제안을_허용한다() {
        when(narrations.findState(contentId))
                .thenReturn(Optional.of(NarrationState.notRequested(contentId, episode.updatedAt())));
        when(proposals.createPending(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.suggest(
                contentId, "reader", NarrationVoiceOption.CHARON.voiceId()
        ).status())
                .isEqualTo(NarrationProposalStatus.PENDING);
    }

    @Test
    void 관리자가_허용_voice로_승인하면_같은_transaction에서_generation을_고정한다() {
        String voiceId = NarrationVoiceOption.ACHIRD.voiceId();
        NarrationProposal approved = approved(voiceId);
        when(proposals.approvePending(proposalId, voiceId, "admin", NOW)).thenReturn(approved);
        NarrationState pending = new NarrationState(
                contentId, UUID.randomUUID(), episode.updatedAt(), NarrationStatus.PENDING,
                voiceId, null, NOW, false
        );
        when(narrations.resetToPending(eq(contentId), any(), eq(episode.updatedAt()), eq(voiceId), eq(NOW)))
                .thenReturn(pending);

        NarrationProposal result = service.approve(proposalId, voiceId, "admin");

        assertThat(result).isEqualTo(approved);
        assertThat(result.preferredVoiceId()).isEqualTo(NarrationVoiceOption.KORE.voiceId());
        assertThat(result.selectedVoiceId()).isEqualTo(voiceId);
        assertThat(events).containsExactly(new NarrationGenerationRequested(
                contentId, pending.generationId(), episode.updatedAt(), voiceId
        ));
        verify(contents).findByIdForUpdate(contentId);
    }

    @Test
    void 허용목록_밖의_voice는_DB를_바꾸기_전에_거절한다() {
        assertThatThrownBy(() -> service.approve(proposalId, "ko-KR-Chirp3-HD-Puck", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("선택할 수 없는");
        verify(proposals, never()).approvePending(any(), any(), any(), any());
        verify(narrations, never()).resetToPending(any(), any(), any(), any(), any());
    }

    private NarrationState state(NarrationStatus status) {
        return new NarrationState(
                contentId, UUID.randomUUID(), episode.updatedAt(), status,
                NarrationVoiceOption.KORE.voiceId(), status == NarrationStatus.FAILED ? "실패" : null,
                NOW, status == NarrationStatus.READY
        );
    }

    private NarrationProposal approved(String voiceId) {
        return new NarrationProposal(
                proposalId, contentId, NarrationProposalStatus.APPROVED,
                "reader", NOW.minusSeconds(1), NarrationVoiceOption.KORE.voiceId(),
                voiceId, "admin", NOW
        );
    }
}
