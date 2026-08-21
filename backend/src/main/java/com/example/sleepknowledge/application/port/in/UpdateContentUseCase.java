package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;

import java.util.UUID;

public interface UpdateContentUseCase {

    Episode updateContent(UUID contentId, EpisodeDraft draft);
}
