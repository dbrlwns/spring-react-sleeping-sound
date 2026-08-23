package com.example.sleepknowledge.application.port.out;

import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;

import java.util.List;

/** macOS say, 클라우드 TTS API, 자체 모델이 구현할 수 있는 outbound port입니다. */
public interface SpeechSynthesisPort {

    List<Voice> findAvailableVoices();

    AudioContent synthesize(String script, NarrationOptions options, Voice voice);
}
