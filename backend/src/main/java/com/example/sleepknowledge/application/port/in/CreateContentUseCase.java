package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;

public interface CreateContentUseCase {

    Episode createContent(EpisodeDraft draft);
}
