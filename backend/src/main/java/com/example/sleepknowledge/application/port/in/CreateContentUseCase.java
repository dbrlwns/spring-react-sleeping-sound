package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.Episode;
import com.example.sleepknowledge.domain.model.EpisodeDraft;

public interface CreateContentUseCase {

    /** HTTP adapter가 인증된 principal 이름을 전달하며, 요청 본문의 작성자 값은 받지 않습니다. */
    Episode createContent(EpisodeDraft draft, String authorUsername);
}
