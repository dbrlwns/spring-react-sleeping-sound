package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.NarrationProposal;

import java.util.Optional;
import java.util.UUID;

public interface SuggestNarrationUseCase {

    NarrationProposal suggest(UUID contentId, String requestedBy, String preferredVoiceId);

    Optional<NarrationProposal> findLatest(UUID contentId);
}
