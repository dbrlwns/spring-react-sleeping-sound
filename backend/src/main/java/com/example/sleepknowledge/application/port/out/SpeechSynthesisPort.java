package com.example.sleepknowledge.application.port.out;

import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;

import java.util.List;

/** 음성 합성 adapter가 구현하는 outbound port입니다. */
public interface SpeechSynthesisPort {

    List<Voice> findAvailableVoices();

    AudioContent synthesize(String script, NarrationOptions options, Voice voice);
}
