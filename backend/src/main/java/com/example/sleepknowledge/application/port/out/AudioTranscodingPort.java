package com.example.sleepknowledge.application.port.out;

import com.example.sleepknowledge.domain.model.AudioContent;

/** 합성용 중간 오디오를 저장·전송용 MP3로 바꾸는 outbound port입니다. */
public interface AudioTranscodingPort {

    /** 유료 합성을 시작하기 전에 로컬 인코더를 실행할 수 있는지 확인합니다. */
    void verifyAvailable();

    AudioContent encodeMp3(AudioContent linear16Wav);
}
