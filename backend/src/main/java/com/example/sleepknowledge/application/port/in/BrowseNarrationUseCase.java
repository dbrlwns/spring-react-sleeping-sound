package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationState;

import java.util.UUID;

public interface BrowseNarrationUseCase {

    NarrationState getNarrationStatus(UUID contentId);

    AudioContent getNarrationAudio(UUID contentId);
}
