package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;

import java.util.UUID;

/** 저장된 콘텐츠 한 편의 원고를 음성으로 만드는 보조 유스케이스입니다. */
public interface GenerateNarrationUseCase {

    AudioContent generateNarration(UUID contentId, NarrationOptions options);
}
