package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.NarrationProposal;
import com.example.sleepknowledge.domain.model.NarrationProposalStatus;
import com.example.sleepknowledge.domain.model.Voice;

import java.util.List;
import java.util.UUID;

public interface ReviewNarrationProposalUseCase {

    List<NarrationProposal> list(NarrationProposalStatus status);

    List<Voice> listVoiceOptions();

    NarrationProposal approve(UUID proposalId, String voiceId, String administrator);

    NarrationProposal reject(UUID proposalId, String administrator);
}
